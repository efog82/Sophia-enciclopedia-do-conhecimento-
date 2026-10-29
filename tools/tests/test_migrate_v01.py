"""Unit tests of safeguards/rollback with a fake ADB; not Android integration tests."""
import contextlib
import io
from pathlib import Path
import sqlite3
import sys
import tarfile
import tempfile
import unittest
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import migrate_v01 as migration


def snapshot(root,version=1):
    directory=root/"databases";directory.mkdir(parents=True,exist_ok=True)
    database=directory/"sophia.db"
    with contextlib.closing(sqlite3.connect(database)) as connection:
        connection.executescript("CREATE TABLE IF NOT EXISTS study_notes(id TEXT,title TEXT,body TEXT,workId TEXT,createdAt INTEGER,updatedAt INTEGER);CREATE TABLE IF NOT EXISTS favorites(workId TEXT,addedAt INTEGER);CREATE TABLE IF NOT EXISTS book_files(id TEXT);")
        if not connection.execute("SELECT count(*) FROM study_notes").fetchone()[0]:
            connection.execute("INSERT INTO study_notes VALUES('n','Título','  espaços e ação  ','w',1,2)")
            connection.execute("INSERT INTO favorites VALUES('w',3)")
        connection.execute("PRAGMA user_version="+str(version));connection.commit()
    preferences=root/"files/datastore/sophia.preferences_pb";preferences.parent.mkdir(parents=True,exist_ok=True);preferences.write_bytes(b'unit-fixture-preferences')
    return migration.make_restore_tar(root)


class FakeDevice:
    def __init__(self,blob,root,fail_install=False):self.blob=blob;self.root=root;self.events=[];self.current=1;self.fail_install=fail_install
    def run(self,*args,data=None,check=True):
        self.events.append(args)
        if args[:3]==("shell","pm","path"):return b"package:/data/app/original/base.apk\n"
        if args[0]=="pull":Path(args[2]).write_bytes(b"original-apk");return b""
        if args[0]=="install":
            if self.fail_install and Path(args[1]).name=="new.apk":raise RuntimeError("Simulated install failure")
            self.current=1 if Path(args[1]).name=="Sophia-original.apk" else 2
        return b""
    def stop(self):pass
    def start(self):
        if self.current==2:
            directory=self.root/"current";directory.mkdir(exist_ok=True)
            migration.extract_snapshot(self.blob,directory)
            self.blob=snapshot(directory,2)
    def snapshot(self):return self.blob
    def restore(self,blob):self.blob=blob


def info(path):return {"package":migration.PACKAGE,"versionCode":1 if Path(path).name=="Sophia-original.apk" else 2,"debuggable":True}


class MigrationSafeguards(unittest.TestCase):
    def setUp(self):self.tmp=tempfile.TemporaryDirectory();self.root=Path(self.tmp.name);self.new=self.root/"new.apk";self.new.write_bytes(b"new-apk");self.source=self.root/"source";self.source.mkdir();self.blob=snapshot(self.source)
    def tearDown(self):self.tmp.cleanup()
    def test_validated_snapshot_preserves_unicode_whitespace_and_preferences(self):
        target=self.root/"copy";target.mkdir();result=migration.extract_snapshot(self.blob,target);self.assertEqual(1,result["study_notes"]["count"]);self.assertEqual(1,result["favorites"]["count"]);self.assertTrue(result["preferences"])
        with contextlib.closing(sqlite3.connect(target/"databases/sophia.db")) as db:self.assertEqual("  espaços e ação  ",db.execute("SELECT body FROM study_notes").fetchone()[0])
    def test_archive_traversal_is_rejected_before_writing(self):
        blob=io.BytesIO()
        with tarfile.open(fileobj=blob,mode="w") as archive:
            entry=tarfile.TarInfo("../outside");entry.size=1;archive.addfile(entry,io.BytesIO(b"x"))
        with self.assertRaises(ValueError):migration.extract_snapshot(blob.getvalue(),self.root/"copy")
        self.assertFalse((self.root.parent/"outside").exists())
    def test_backup_only_never_uninstalls(self):
        device=FakeDevice(self.blob,self.root)
        with patch.object(migration,"apk_info",side_effect=info),contextlib.redirect_stdout(io.StringIO()):migration.migrate(device,self.new,self.root/"backup")
        self.assertFalse(any(event[0]=="uninstall" for event in device.events));self.assertTrue((self.root/"backup/dados-validados.tar").exists())
    def test_user_cancellation_keeps_original_installed(self):
        device=FakeDevice(self.blob,self.root)
        with patch.object(migration,"apk_info",side_effect=info),patch("builtins.input",return_value="cancelar"),contextlib.redirect_stdout(io.StringIO()):migration.migrate(device,self.new,self.root/"backup",reinstall=True)
        self.assertFalse(any(event[0]=="uninstall" for event in device.events))
    def test_install_failure_restores_original_apk_and_snapshot(self):
        device=FakeDevice(self.blob,self.root,True)
        with patch.object(migration,"apk_info",side_effect=info),contextlib.redirect_stdout(io.StringIO()):
            with self.assertRaises(RuntimeError):migration.migrate(device,self.new,self.root/"backup",reinstall=True,confirmed=True)
        self.assertEqual(1,device.current);self.assertTrue((self.root/"backup/Sophia-original.apk").is_file())
    def test_success_verifies_all_personal_fingerprints(self):
        device=FakeDevice(self.blob,self.root)
        with patch.object(migration,"apk_info",side_effect=info),patch.object(migration.time,"sleep"),contextlib.redirect_stdout(io.StringIO()):migration.migrate(device,self.new,self.root/"backup",reinstall=True,confirmed=True)
        import json
        report=json.loads((self.root/"backup/verificacao.json").read_text());self.assertTrue(report["verified"]);self.assertEqual(report["source"]["study_notes"],report["destination"]["study_notes"]);self.assertEqual(report["source"]["preferences"],report["destination"]["preferences"])


if __name__=="__main__":unittest.main()
