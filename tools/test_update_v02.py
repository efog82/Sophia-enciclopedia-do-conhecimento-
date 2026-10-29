#!/usr/bin/env python3
"""CI emulator only: verify a normal signed 0.2.0 -> 0.3.0 install -r, without uninstall."""
import argparse
from contextlib import closing
import hashlib
import json
from pathlib import Path
import sqlite3
import tempfile
import time
from migrate_v01 import Device, PACKAGE, apk_info, extract_snapshot, make_restore_tar


def tables(root):
    with closing(sqlite3.connect(root / 'databases/sophia.db')) as db:
        names=[r[0] for r in db.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'room_master_table'")]
        return {name:sorted(db.execute('SELECT * FROM "'+name+'"').fetchall(), key=repr) for name in names}


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--apk',type=Path,required=True);args=parser.parse_args()
    assert apk_info(args.apk)['versionCode']==3
    device=Device(emulator_only=True)
    device.stop()
    with tempfile.TemporaryDirectory(prefix='sophia-update-v02-') as temp:
        root=Path(temp)/'before';root.mkdir();extract_snapshot(device.snapshot(),root)
        text='Documento original de ensaio para atualização direta.\n'.encode()
        local=root/'files/books/ci-update.txt';local.parent.mkdir(parents=True,exist_ok=True);local.write_bytes(text)
        locator=json.dumps(dict(version=1,unit=0,fraction=.35),separators=(',',':'))
        with closing(sqlite3.connect(root/'databases/sophia.db')) as db:
            assert db.execute('PRAGMA user_version').fetchone()[0]==2
            db.execute("INSERT INTO editions(id,workId,label,rightsStatus) VALUES ('ci-update-edition','spirits','Ensaio','USER_AUTHORISED_NOT_VERIFIED')")
            db.execute("INSERT INTO book_files(id,editionId,uri,mimeType,sha256,rightsStatus,sizeBytes,displayName,addedAt) VALUES ('ci-update-file','ci-update-edition',?,'text/plain',?,'USER_AUTHORISED_NOT_VERIFIED',?,'ci-update.txt',123)",(f'file:///data/user/0/{PACKAGE}/files/books/ci-update.txt',hashlib.sha256(text).hexdigest(),len(text)))
            db.execute("INSERT INTO reading_progress(fileId,locatorJson,progressPermille,updatedAt) VALUES ('ci-update-file',?,350,124)",(locator,))
            db.execute("INSERT INTO bookmarks(id,fileId,locatorJson,label,createdAt) VALUES ('ci-update-bookmark','ci-update-file',?,'Posição anterior',125)",(locator,))
            db.execute("INSERT INTO study_notes(id,title,body,workId,fileId,locatorJson,excerpt,createdAt,updatedAt) VALUES ('ci-update-note','Nota ligada','  Texto pessoal com espaços.  ','spirits','ci-update-file',?,'Documento original',126,127)",(locator,))
            db.execute("INSERT OR IGNORE INTO favorites(workId,addedAt) VALUES ('spirits',128)")
            db.execute("INSERT INTO study_collections(id,name,readingList,createdAt) VALUES ('ci-update-list','Lista anterior',1,129)")
            db.execute("INSERT INTO collection_works(collectionId,workId,position) VALUES ('ci-update-list','spirits',0)")
            db.commit()
        expected=tables(root)
        preferences={str(p.relative_to(root)):p.read_bytes() for p in (root/'files/datastore').glob('*.preferences_pb')}
        device.restore(make_restore_tar(root))
        device.run('install','-r',str(args.apk.resolve()))
        device.start()
        after=Path(temp)/'after'
        for attempt in range(20):
            time.sleep(.5);device.stop();after.mkdir(exist_ok=True);extract_snapshot(device.snapshot(),after)
            if tables(after)==expected:break
            device.start()
        assert tables(after)==expected,'Uma tabela foi alterada na atualização direta'
        assert (after/'files/books/ci-update.txt').read_bytes()==text
        assert preferences=={str(p.relative_to(after)):p.read_bytes() for p in (after/'files/datastore').glob('*.preferences_pb')}
        installed=device.run('shell','dumpsys','package',PACKAGE).decode()
        assert 'versionName=0.3.0' in installed
        device.start()
        print('ATUALIZAÇÃO DIRETA v0.2.0 → v0.3.0 APROVADA: assinatura, tabelas Room, ficheiro, marcador, posição, notas, favoritos, coleção e preferências preservados.')


if __name__=='__main__':main()
