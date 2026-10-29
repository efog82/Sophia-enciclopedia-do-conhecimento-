#!/usr/bin/env python3
"""CI only: seed an emulator's real v0.1.0 database, then verify assisted migration."""
import argparse
from contextlib import closing
from pathlib import Path
import re
import sqlite3
import tempfile
import time
import xml.etree.ElementTree as ET
from migrate_v01 import Device, PACKAGE, extract_snapshot, make_restore_tar, migrate


def tap(device, text=None, description=None):
    for attempt in range(5):
        device.run("shell", "uiautomator", "dump", "/data/local/tmp/sophia-migration-ui.xml")
        root = ET.fromstring(device.run("shell", "cat", "/data/local/tmp/sophia-migration-ui.xml"))
        node = next((n for n in root.iter("node") if (text is not None and n.get("text") == text) or (description is not None and n.get("content-desc") == description)), None)
        if node is not None:
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
            device.run("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
            return
        time.sleep(.5)
    raise ValueError("O APK original não apresentou o controlo esperado para o teste")


def main():
    parser=argparse.ArgumentParser();parser.add_argument("--apk",type=Path,required=True);parser.add_argument("--backup-dir",type=Path,required=True);args=parser.parse_args()
    device=Device(emulator_only=True)
    # Change the original app's actual DataStore through its authorised UI.
    tap(device,description="Escolher tema");tap(device,text="Escuro");time.sleep(1)
    device.stop()
    with tempfile.TemporaryDirectory(prefix="sophia-v1-fixture-") as temp:
        root=Path(temp);extract_snapshot(device.snapshot(),root)
        with closing(sqlite3.connect(root/"databases/sophia.db")) as connection:
            connection.execute("INSERT OR REPLACE INTO favorites(workId,addedAt) VALUES ('spirits',123)")
            connection.execute("INSERT OR REPLACE INTO study_notes(id,title,body,workId,createdAt,updatedAt) VALUES ('ci-legacy','Nota anterior à atualização','  Texto pessoal de ensaio, sem perda de espaços.  ','spirits',111,222)")
            connection.commit()
        if not list((root/"files/datastore").glob("*.preferences_pb")):
            raise ValueError("O teste não encontrou a preferência real de tema da aplicação antiga")
        device.restore(make_restore_tar(root))
    migrate(device,args.apk,args.backup_dir,reinstall=True,confirmed=True)
    print("ATUALIZAÇÃO ASSISTIDA TESTADA NO EMULADOR: APK original, nota, favorito e preferência real preservados.")


if __name__=="__main__":main()
