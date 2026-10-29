# Migrar a instalação v0.1.0 sem perder os dados

## Porque é necessário este procedimento

O teste real encontrou uma incompatibilidade de assinatura entre o APK v0.1.0 publicado e a nova compilação. O workflow antigo não conservou a chave privada no local de cache indicado. O certificado público dentro do APK não permite recuperar essa chave. **Não é possível uma atualização direta por cima dessa instalação**, nem se deve contornar a segurança do Android.

A release v0.1.0 continua intacta. O identificador da aplicação mantém-se `com.sophia.enciclopedia`. Para utilizadores com dados nessa versão, a alternativa é guardar e validar os dados, reinstalar e restaurá-los. O procedimento abaixo exige um computador **apenas uma vez**; a aplicação e o leitor continuam independentes de PC, servidor e rede.

## Requisitos

- APK debug Sophia v0.2.0 e o código desta versão, incluindo a pasta `tools`, ou o arquivo `Sophia-v0.2.0-migracao.zip` extraído.
- Python 3.10 ou superior e [Android Platform Tools / ADB oficial](https://developer.android.com/tools/releases/platform-tools).
- Cabo USB, depuração USB ativada no Android e autorização explícita do computador no telemóvel. Sem root.
- A v0.1.0 original instalada, de tipo debug, com esquema Room 1 e sem ficheiros de livros importados (a versão original não tinha importação). A ferramenta verifica estas condições e recusa outros casos.
- Não desinstalar a aplicação antiga manualmente e não limpar os dados.

## Primeiro: só guardar e verificar o backup

Na raiz do projeto:

```sh
python3 tools/migrate_v01.py --apk /caminho/Sophia-v0.2.0-debug.apk
```

No Windows, usa `py -3` em vez de `python3`. Se `adb` não estiver no PATH, acrescenta `--adb "C:\platform-tools\adb.exe"`. Com vários dispositivos, usa `--serial SERIAL`.

A ferramenta copia o APK realmente instalado e os dados privados, verifica a integridade SQLite, consolida o WAL e calcula impressões dos conteúdos das notas/favoritos e das preferências. Por omissão, **não desinstala nada**. O backup fica numa pasta nova em `Sophia-backups` dentro da pasta pessoal; pode indicar `--backup-dir` com uma pasta nova.

O backup contém dados pessoais em texto/base de dados. Guarda-o em privado, fora de pastas partilhadas. Nunca o publiques no GitHub. Nenhum dado é enviado a serviços externos pela ferramenta.

## Migrar após validar o backup

```sh
python3 tools/migrate_v01.py --apk /caminho/Sophia-v0.2.0-debug.apk --reinstall
```

No Windows, também podes arrastar o APK para `tools/migrar-v01.bat`.

Só depois de obter e validar uma cópia, a ferramenta pede para escrever **MIGRAR**. Explica que haverá reinstalação porque a assinatura antiga não está disponível. Se cancelares, conserva e reabre a versão antiga.

Após confirmação:

1. Desinstala o APK antigo, já guardado no backup.
2. Instala o APK novo e restaura a cópia validada na pasta privada da mesma aplicação.
3. Abre a aplicação, que executa a migração Room 1→2 e acrescenta o catálogo.
4. Verifica que os conteúdos das notas/favoritos e os ficheiros de preferências coincidem com os anteriores; conserva o backup.
5. Se detetar uma falha, tenta repor automaticamente o APK original e os seus dados.

Mantém o cabo ligado e o telemóvel desbloqueado durante o procedimento. Uma desconexão pode impedir o rollback automático; o backup permanece no computador.

## Recuperação manual se o dispositivo se desligar

Os ficheiros `Sophia-original.apk`, `dados-validados.tar` e `verificacao.json` ficam no backup. Não apagues essa pasta. A recuperação repõe o estado anterior; não inclui notas criadas posteriormente na versão nova.

Num terminal **cmd** do Windows ou shell Linux/macOS, com o dispositivo novamente autorizado:

```sh
adb uninstall com.sophia.enciclopedia
adb install /caminho/do/backup/Sophia-original.apk
adb shell -T run-as com.sophia.enciclopedia tar -xf - < /caminho/do/backup/dados-validados.tar
adb shell am start -W -n com.sophia.enciclopedia/.MainActivity
```

Não uses a recuperação se existirem novos dados sem os guardar primeiro. No PowerShell, abre `cmd` para o redirecionamento binário acima; evita converter o TAR em texto.

## Limites

Este é um mecanismo específico de transição da v0.1.0 debug, não a funcionalidade geral de backup/restauro da aplicação. Não serve para APKs não-debug, instalações divididas ou versões/dados diferentes. Não garante recuperação se o dispositivo perder dados físicos ou se o computador também perder o backup.

A correção da nova assinatura evita repetir a geração silenciosa de chaves: existe caminho explícito, cache próprio, segredo opcional e bloqueio se a chave publicada não puder ser reposta. Para versões futuras, o responsável pelo repositório deve manter uma cópia segura permanente da nova keystore; um cache com retenção limitada não é um arquivo definitivo.
