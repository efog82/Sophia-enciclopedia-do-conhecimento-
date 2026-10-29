#!/usr/bin/env python3
"""Opt-in migration for debug Sophia v0.1.0 when its original signing key was lost.
Uses only authorised Android ADB/run-as, never root, signature bypass or network.
Default: creates and validates a local backup only. --reinstall asks for confirmation.
"""
import argparse
from contextlib import closing
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import sqlite3
import struct
import subprocess
import tarfile
import tempfile
import time
import uuid
import zipfile

PACKAGE = "com.sophia.enciclopedia"
COMPONENT = PACKAGE + "/.MainActivity"
PERSONAL = {"study_notes": "id,title,body,workId,createdAt,updatedAt", "favorites": "workId,addedAt"}


def apk_info(path):
    with zipfile.ZipFile(path) as archive:
        data = archive.read("AndroidManifest.xml")
    def short(offset): return struct.unpack_from("<H", data, offset)[0]
    def integer(offset): return struct.unpack_from("<I", data, offset)[0]
    strings, result, offset = [], {}, 8
    while offset < len(data):
        kind, header, size = struct.unpack_from("<HHI", data, offset)
        if size < 8 or offset + size > len(data):
            raise ValueError("Manifesto APK inválido")
        if kind == 1:
            count, flags, start = integer(offset + 8), integer(offset + 16), offset + integer(offset + 20)
            for index in range(count):
                pointer = start + integer(offset + header + index * 4)
                if flags & 256:
                    def length(at):
                        value = data[at]
                        return ((((value & 127) << 8) | data[at + 1]), at + 2) if value & 128 else (value, at + 1)
                    _, pointer = length(pointer)
                    length_value, pointer = length(pointer)
                    strings.append(data[pointer:pointer + length_value].decode("utf-8"))
                else:
                    length_value, pointer = short(pointer), pointer + 2
                    if length_value & 32768:
                        length_value, pointer = ((length_value & 32767) << 16) | short(pointer), pointer + 2
                    strings.append(data[pointer:pointer + length_value * 2].decode("utf-16-le"))
        elif kind == 258:
            name, count = strings[integer(offset + 20)], short(offset + 28)
            base, attribute_size = offset + 16 + short(offset + 24), short(offset + 26)
            attributes = {}
            for index in range(count):
                at = base + index * attribute_size
                key, raw, value_type, value = strings[integer(at + 4)], integer(at + 8), data[at + 15], integer(at + 16)
                attributes[key] = strings[raw] if raw != 0xffffffff else strings[value] if value_type == 3 else value
            if name in ("manifest", "application"):
                result.update(attributes)
        offset += size
    return result


def extract_snapshot(blob, directory):
    with tarfile.open(fileobj=io.BytesIO(blob), mode="r:*") as archive:
        total = 0
        for member in archive.getmembers():
            name = PurePosixPath(member.name)
            if name.is_absolute() or ".." in name.parts:
                raise ValueError("Caminho inseguro no arquivo de dados")
            if not member.isfile() or not name.parts or name.parts[0] not in ("databases", "files", "shared_prefs"):
                continue
            total += member.size
            if total > 1024 ** 3:
                raise ValueError("Backup superior a 1 GiB: requer revisão manual")
            target = directory.joinpath(*name.parts)
            target.parent.mkdir(parents=True, exist_ok=True)
            with archive.extractfile(member) as source, target.open("wb") as output:
                output.write(source.read())
            target.chmod(0o600)
    database = directory / "databases/sophia.db"
    if not database.is_file():
        raise ValueError("A base de dados Sophia não existe. Abra primeiro a aplicação antiga.")
    # SQLite reads WAL consistently, then produces a clean independent database.
    clean = directory / "validated.db"
    with closing(sqlite3.connect(database)) as connection, closing(sqlite3.connect(clean)) as output:
        if connection.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
            raise ValueError("A base de dados não passou a verificação de integridade")
        connection.backup(output)
    database.write_bytes(clean.read_bytes())
    clean.unlink()
    for suffix in ("-wal", "-shm"):
        Path(str(database) + suffix).unlink(missing_ok=True)
    result=personal_fingerprint(database)
    result["preferences"]={str(p.relative_to(directory)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((directory/"files/datastore").glob("*.preferences_pb"))}
    return result


def personal_fingerprint(database):
    with closing(sqlite3.connect(database)) as connection:
        version = connection.execute("PRAGMA user_version").fetchone()[0]
        result = {"schema": version}
        for table, columns in PERSONAL.items():
            rows = connection.execute("SELECT " + columns + " FROM " + table + " ORDER BY 1").fetchall()
            result[table] = {"count": len(rows), "sha256": hashlib.sha256(json.dumps(rows, ensure_ascii=False).encode("utf-8")).hexdigest()}
        result["book_files"] = connection.execute("SELECT count(*) FROM book_files").fetchone()[0]
        return result


def make_restore_tar(directory):
    output = io.BytesIO()
    with tarfile.open(fileobj=output, mode="w") as archive:
        for folder in ("databases", "files", "shared_prefs"):
            root = directory / folder
            if root.exists():
                for path in sorted(root.rglob("*")):
                    if path.is_file():
                        archive.add(path, arcname=str(path.relative_to(directory)), recursive=False)
    return output.getvalue()


class Device:
    def __init__(self, adb="adb", serial=None, emulator_only=False):
        self.command = [adb] + (["-s", serial] if serial else [])
        state = self.run("get-serialno").decode().strip()
        if not state or state == "unknown" or (emulator_only and not state.startswith("emulator-")):
            raise ValueError("Selecione um único dispositivo Android autorizado (emulador no CI)")
        self.serial = state
    def run(self, *args, data=None, check=True):
        process = subprocess.run(self.command + list(args), input=data, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if check and process.returncode:
            raise RuntimeError("ADB falhou em " + args[0] + ": " + process.stderr.decode(errors="replace").strip())
        return process.stdout
    def stop(self): self.run("shell", "am", "force-stop", PACKAGE)
    def start(self): self.run("shell", "am", "start", "-W", "-n", COMPONENT)
    def snapshot(self): return self.run("exec-out", "run-as", PACKAGE, "tar", "-cf", "-", ".")
    def restore(self, blob):
        self.run("shell", "run-as", PACKAGE, "rm", "-f", "databases/sophia.db-wal", "databases/sophia.db-shm")
        self.run("shell", "-T", "run-as", PACKAGE, "tar", "-xf", "-", data=blob)


def migrate(device, apk, backup_dir, reinstall=False, confirmed=False):
    info = apk_info(apk)
    if info.get("package") != PACKAGE or info.get("versionCode") != 2 or not info.get("debuggable"):
        raise ValueError("Escolha o APK debug oficial Sophia v0.2.0, com o identificador original")
    paths = device.run("shell", "pm", "path", PACKAGE).decode().strip().splitlines()
    if len(paths) != 1 or not paths[0].startswith("package:"):
        raise ValueError("A ferramenta exige uma instalação antiga de APK único")
    device.run("exec-out", "run-as", PACKAGE, "id")  # Fails safely unless the device authorises debug access.
    backup_dir.mkdir(parents=True, exist_ok=False)
    backup_dir.chmod(0o700)
    original_apk = backup_dir / "Sophia-original.apk"
    device.run("pull", paths[0][8:], str(original_apk))
    original_info = apk_info(original_apk)
    if original_info.get("versionCode") != 1:
        raise ValueError("A migração automática destina-se apenas à v0.1.0; nada foi desinstalado")
    device.stop()
    original_blob = device.snapshot()
    (backup_dir / "dados-originais.tar").write_bytes(original_blob)
    (backup_dir / "dados-originais.tar").chmod(0o600)
    validated = backup_dir / "snapshot"
    validated.mkdir()
    before = extract_snapshot(original_blob, validated)
    if before["schema"] != 1 or before["book_files"] != 0:
        raise ValueError("Esta base não corresponde à v0.1.0 sem leitor. Nada foi desinstalado; os dados estão guardados")
    restore_blob = make_restore_tar(validated)
    (backup_dir / "dados-validados.tar").write_bytes(restore_blob)
    (backup_dir / "dados-validados.tar").chmod(0o600)
    manifest = {"package": PACKAGE, "source": before, "source_apk_sha256": hashlib.sha256(original_apk.read_bytes()).hexdigest(), "new_apk_sha256": hashlib.sha256(Path(apk).read_bytes()).hexdigest()}
    (backup_dir / "verificacao.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print("Backup validado:", backup_dir)
    print("Notas:", before["study_notes"]["count"], "· Favoritos:", before["favorites"]["count"])
    print("O backup contém dados pessoais. Guarde-o em privado e não o publique no GitHub.")
    if not reinstall:
        device.start()
        print("Só foi criado o backup. A versão antiga foi conservada. Use --reinstall para migrar.")
        return
    if not confirmed and input("A reinstalação usará este backup para conservar os dados. Escreva MIGRAR para continuar: ").strip() != "MIGRAR":
        device.start()
        print("Migração cancelada; a aplicação antiga permanece instalada.")
        return
    removed = False
    try:
        device.run("uninstall", PACKAGE)
        removed = True
        device.run("install", str(Path(apk).resolve()))
        device.restore(restore_blob)
        verified = False
        for attempt in range(8):
            device.start()
            time.sleep(1.5)
            device.stop()
            with tempfile.TemporaryDirectory(prefix="sophia-check-") as temporary:
                after = extract_snapshot(device.snapshot(), Path(temporary))
            if after["schema"] == 2:
                if any(before[name] != after[name] for name in (*PERSONAL, "preferences")):
                    raise ValueError("As notas/favoritos não coincidem com o backup")
                verified = True
                break
        if not verified:
            raise ValueError("A aplicação não concluiu a migração Room para o esquema 2")
        manifest["destination"] = after
        manifest["verified"] = True
        (backup_dir / "verificacao.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
        device.start()
        print("MIGRAÇÃO VERIFICADA: notas e favoritos conservados; esquema Room 1→2. Backup mantido.")
    except Exception:
        if removed:
            print("A migração falhou. A repor o APK original e o backup validado…")
            device.run("uninstall", PACKAGE, check=False)
            device.run("install", str(original_apk))
            device.restore(restore_blob)
            device.start()
            print("APK original e dados repostos. O backup foi conservado.")
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial")
    parser.add_argument("--backup-dir", type=Path)
    parser.add_argument("--reinstall", action="store_true")
    parser.add_argument("--confirm-reinstall", action="store_true", help="Confirma explicitamente a reinstalação após validar o backup")
    parser.add_argument("--emulator-only", action="store_true")
    args = parser.parse_args()
    backup = args.backup_dir or Path.home() / "Sophia-backups" / (time.strftime("%Y%m%d-%H%M%S-") + str(uuid.uuid4())[:8])
    migrate(Device(args.adb, args.serial, args.emulator_only), args.apk, backup, args.reinstall, args.confirm_reinstall)


if __name__ == "__main__":
    main()
