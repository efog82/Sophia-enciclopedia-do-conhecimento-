"""Obtain only the two individually reviewed, public-domain Portuguese editions.
This is a maintainer preparation step, never an app download operation.
Unmodified EPUBs retain their Project Gutenberg notices and licence.
"""
import hashlib,json,urllib.request,zipfile,io,xml.etree.ElementTree as ET
from pathlib import Path
BOOKS=[
 dict(id='pg-26776',gutenberg=26776,title='A philosophia da natureza dos naturalistas',author='Antero de Quental',authorDied=1891,tradition='portuguese-philosophy',description='Ensaio filosófico português sobre naturalismo e metafísica. Texto histórico: as suas teses não representam consenso científico atual.'),
 dict(id='pg-23526',gutenberg=23526,title='A Velhice do Padre Eterno',author='Guerra Junqueiro',authorDied=1923,tradition='christianity',description='Obra poética portuguesa de crítica religiosa e institucional. Representa a perspetiva do autor, não uma apresentação normativa do Cristianismo.'),
]
def main():
 out=Path('app/src/main/assets/books');out.mkdir(parents=True,exist_ok=True)
 for book in BOOKS:
  book['source']=f"https://www.gutenberg.org/ebooks/{book['gutenberg']}"
  url=book['source']+'.epub.noimages'
  req=urllib.request.Request(url,headers={'User-Agent':'Sophia-OpenSource-Library/0.3 (two reviewed public-domain books)'})
  with urllib.request.urlopen(req,timeout=90) as response:
   data=response.read(8*1024*1024+1)
  assert len(data)<=8*1024*1024
  with zipfile.ZipFile(io.BytesIO(data)) as z:
   assert z.testzip() is None
   assert z.read('mimetype')==b'application/epub+zip'
   xml='\n'.join(z.read(n).decode('utf-8','replace') for n in z.namelist() if n.endswith(('.opf','.html','.xhtml')))
   assert 'Project Gutenberg' in xml and 'PROJECT GUTENBERG' in xml.upper()
   assert ('Quental' in xml if book['gutenberg']==26776 else 'Junqueiro' in xml)
   assert book['title'].split()[1].lower() in xml.lower()
   opfs=[n for n in z.namelist() if n.endswith('.opf')];assert opfs
   root=ET.fromstring(z.read(opfs[0]));languages=[n.text for n in root.iter() if n.tag.endswith('}language')]
   assert any((v or '').lower().startswith('pt') for v in languages),languages
  book.update(file=book['id']+'.epub',sha256=hashlib.sha256(data).hexdigest(),language='pt-PT',spelling='Português de Portugal histórico; ortografia original conservada.',rights='PUBLIC_DOMAIN_PT_US',rightsBasis=f"Obra original de autor português falecido em {book['authorDied']}; mais de 70 anos após a morte. Edição histórica disponibilizada pelo Project Gutenberg como domínio público nos EUA, com créditos da Biblioteca Nacional de Portugal. EPUB e licença mantidos sem alterações.")
  (out/book['file']).write_bytes(data)
  print(book['id'],len(data),book['sha256'])
 (out/'catalog.json').write_text(json.dumps(BOOKS,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
if __name__=='__main__':main()
