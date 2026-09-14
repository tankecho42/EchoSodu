#!/usr/bin/env python3
"""Reproducible no-download build using official Android SDK tools and JDK 17."""
import os,subprocess,secrets,pathlib,zipfile,hashlib,json,shutil
ROOT=pathlib.Path(__file__).resolve().parents[1]
SDK=pathlib.Path(os.environ.get('ANDROID_HOME',str(pathlib.Path.home()/'Library/Android/sdk')))
JAVA=pathlib.Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home'))
BT=SDK/'build-tools/35.0.0'; JAR=SDK/'platforms/android-34/android.jar'
BUILD=ROOT/'android/app/build/direct';BUILD.mkdir(parents=True,exist_ok=True)
for p in ['generated','classes','dex']:
    shutil.rmtree(BUILD/p,ignore_errors=True)
    (BUILD/p).mkdir(exist_ok=True)
SRC=ROOT/'android/app/src/main'
def run(args):subprocess.run([str(a) for a in args],check=True,env={**os.environ,'JAVA_HOME':str(JAVA),'PATH':str(JAVA/'bin')+':'+os.environ['PATH']})
manifest=BUILD/'AndroidManifest.xml'
manifest.write_text((SRC/'AndroidManifest.xml').read_text().replace('<manifest ', '<manifest package="com.tankecho.zensudoku" ',1))
run([BT/'aapt','package','-f','-m','-M',manifest,'-S',SRC/'res','-A',SRC/'assets','-I',JAR,'-J',BUILD/'generated','-F',BUILD/'unsigned.apk','--min-sdk-version','26','--target-sdk-version','34','--version-code','15','--version-name','1.10.0'])
sources=sorted((SRC/'java').rglob('*.java'))+sorted((BUILD/'generated').rglob('*.java'))
run([JAVA/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-bootclasspath',str(BT/'core-lambda-stubs.jar')+':'+str(JAR),'-d',BUILD/'classes',*sources])
run([BT/'d8','--release','--min-api','26','--lib',JAR,'--output',BUILD/'dex',*sorted((BUILD/'classes').rglob('*.class'))])
with zipfile.ZipFile(BUILD/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
    for f in (BUILD/'dex').glob('*.dex'):z.write(f,f.name)
run([BT/'zipalign','-f','-p','4',BUILD/'unsigned.apk',BUILD/'aligned.apk'])
KEYDIR=pathlib.Path.home()/'.local/share/zen-sudoku/signing';KEYDIR.mkdir(parents=True,exist_ok=True);KEYDIR.chmod(0o700)
KEY=KEYDIR/'release.jks';PASS=KEYDIR/'password'
if not KEY.exists():
    PASS.write_text(secrets.token_urlsafe(32));PASS.chmod(0o600)
    run([JAVA/'bin/keytool','-genkeypair','-keystore',KEY,'-storepass:file',PASS,'-keypass:file',PASS,'-alias','zensudoku','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Zen Sudoku, O=TankEcho, C=CN'])
    KEY.chmod(0o600)
APK=ROOT/'artifacts/v1.10.0/EchoSodu-1.10.0.apk'
APK.parent.mkdir(parents=True,exist_ok=True)
run([BT/'apksigner','sign','--ks',KEY,'--ks-pass','file:'+str(PASS),'--ks-key-alias','zensudoku','--out',APK,BUILD/'aligned.apk'])
run([BT/'apksigner','verify','--verbose',APK])
meta={'file':APK.name,'size':APK.stat().st_size,'sha256':hashlib.sha256(APK.read_bytes()).hexdigest(),'applicationId':'com.tankecho.zensudoku','version':'1.10.0','minSdk':26,'targetSdk':34}
(ROOT/'artifacts/v1.10.0/apk-metadata.json').write_text(json.dumps(meta,indent=2))
print(json.dumps(meta,indent=2))
