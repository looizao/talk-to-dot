import argparse,os,subprocess,time,xml.etree.ElementTree as ET,re,json
from pathlib import Path
parser=argparse.ArgumentParser(description="Device navigation checks; requires an unlocked, configured phone.")
parser.add_argument('scenario',choices=['music','cold','interrupted'])
parser.add_argument('--dot-name',default='zip',help='Must match the name saved in Talk to Dot')
args=parser.parse_args()
DEVICE=os.environ.get('ANDROID_SERIAL')
root=Path(__file__).resolve().parent.parent/'build'/'device-checks'
root.mkdir(parents=True,exist_ok=True)

def adb(*args):
 return subprocess.run(['adb',*(['-s',DEVICE] if DEVICE else []),*args],capture_output=True,text=True,check=True,timeout=45).stdout

def screen(label):
 adb('shell','uiautomator','dump','/sdcard/window.xml')
 xml=adb('shell','cat','/sdcard/window.xml')
 (root/('stress-'+label+'.xml')).write_text(xml)
 return ET.fromstring(xml)

def has(tree,label):
 return any(n.get('text')==label or n.get('content-desc')==label for n in tree.iter('node'))

def click(tree,label):
 candidates=[n for n in tree.iter('node') if n.get('content-desc')==label]
 if not candidates:candidates=[n for n in tree.iter('node') if n.get('text')==label]
 if not candidates:raise RuntimeError('Missing control: '+label)
 n=candidates[0];x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
 adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))

def start(pkg,component=None):
 if component:adb('shell','am','start','-W','-n',component)
 else:adb('shell','am','start','-W','-a','android.intent.action.MAIN','-c','android.intent.category.LAUNCHER','-p',pkg)

def shortcut():start('local.zipshortcut','local.zipshortcut/.MainActivity')

def prepare_tasks(label):
 start('com.openai.chatgpt','com.openai.chatgpt/.MainActivity')
 tree=screen(label+'-prepare')
 if has(tree,'Tasks'):
  click(tree,'Navegar para cima' if has(tree,'Navegar para cima') else 'Navigate up');tree=screen(label+'-back')
 if not has(tree,'Scheduled'):
  click(tree,'Menu');tree=screen(label+'-menu')
 click(tree,'New chat');tree=screen(label+'-newchat')
 assert has(tree,'Ask ChatGPT'),'Expected ordinary ChatGPT screen'
 click(tree,'Menu');tree=screen(label+'-new-menu')
 click(tree,'Scheduled');tree=screen(label+'-tasks')
 assert has(tree,'Tasks'),'Expected Tasks starting screen'

results=[]
def verify_dot(label):
 time.sleep(3)
 for attempt in range(10):
  tree=screen(label+'-result-'+str(attempt))
  if has(tree,'Message '+args.dot_name) and not has(tree,'Scheduled') and not has(tree,'Tasks'):
   logs=adb('logcat','-d','-s','TalkToDot:D','*:S')
   times=re.findall(r'Navigation completed in (\d+) ms',logs)
   results.append({'scenario':label,'passed':True,'service_ms':int(times[-1]) if times else None})
   print(json.dumps(results[-1]),flush=True)
   return
  time.sleep(.3)
 raise RuntimeError('Did not reach configured Dot in '+label)

scenario=args.scenario
if scenario=='music':
 prepare_tasks('music')
 start('com.google.android.apps.youtube.music')
 tree=screen('music-foreground')
 assert any(n.get('package')=='com.google.android.apps.youtube.music' for n in tree.iter('node'))
 shortcut();verify_dot('music-playing-from-music-app')
elif scenario=='cold':
 prepare_tasks('loaded-cold')
 start('com.android.chrome')
 adb('shell','am','force-stop','com.openai.chatgpt')
 shortcut();verify_dot('loaded-cold-from-chrome')
elif scenario=='interrupted':
 prepare_tasks('interruption')
 adb('shell','am','force-stop','com.openai.chatgpt')
 adb('shell','am start -W -n local.zipshortcut/.MainActivity >/dev/null; sleep 0.1; am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.google.android.apps.youtube.music >/dev/null')
 foreground=adb('shell','dumpsys','activity','activities')
 assert re.search(r'topResumedActivity=.*com.google.android.apps.youtube.music/',foreground),'Music did not stay foreground'
 time.sleep(2)
 foreground=adb('shell','dumpsys','activity','activities')
 assert re.search(r'topResumedActivity=.*com.google.android.apps.youtube.music/',foreground),'Shortcut took over another app'
 print('Other app stayed foreground; no navigation takeover.',flush=True)
 start('com.openai.chatgpt','com.openai.chatgpt/.MainActivity')
 verify_dot('resume-after-switching-to-music')
(root/('stress-'+scenario+'.json')).write_text(json.dumps(results,indent=2))
