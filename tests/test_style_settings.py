"""Navigation contracts and compact-slider value formatting, without an APK."""
import shutil
import unittest
from pathlib import Path
from test_personal_privacy import method, run_java

JAVA = Path(__file__).resolve().parents[1] / 'TMessagesProj/src/main/java'


@unittest.skipUnless(shutil.which('java'), 'Java source launcher required')
class StyleSettingsTests(unittest.TestCase):
    def test_folders_preview_switches_its_native_background(self):
        path = JAVA / 'app/freezgram/FoldersGlassPreview.java'
        self.assertTrue(path.exists(), 'The folder preview must connect a live glass drawable')
        refresh = method(path.read_text(encoding='utf-8'), '    void refresh()')
        run_java('''
class PrivacyCheck {
 Folders folders=new Folders();Object glass=new Object();void invalidate(){}
 REFRESH
 public static void main(String[] args){
  PrivacyCheck p=new PrivacyCheck();
  for(boolean enabled:new boolean[]{false,true,false,true}){
   AppearanceConfig.enabled=enabled;p.refresh();
   if(p.folders.background!=(enabled?p.glass:null))throw new AssertionError("Native folder material is stale");
  }
  p.glass=null;p.refresh();if(p.folders.background!=null)throw new AssertionError("Unsupported renderer fallback");
 }
}
class AppearanceConfig {static boolean enabled;static boolean foldersLiquidGlass(){return enabled;}}
class Folders {Object background;void setGlassPreviewBackground(Object value){background=value;}}
'''.replace('REFRESH',refresh))

    def test_every_style_destination_is_available_without_writing_preferences(self):
        path = JAVA / 'app/freezgram/FreezgramStyleActivity.java'
        self.assertTrue(path.exists(), 'Unified style list is missing')
        route = method(path.read_text(encoding='utf-8'), '    public static BaseFragment editorFor(String key)')
        run_java('''
class PrivacyCheck {
 ROUTE
 public static void main(String[] args) {
  String[] keys={"navigation","header","composer","chatList","messages","gifts","surfaces","glass","dividers","motion","snow"};
  for(String key:keys)if(editorFor(key)==null)throw new AssertionError("Unreachable section: "+key);
  if(editorFor("unknown")!=null || editorFor(null)!=null)throw new AssertionError("Unknown destination");
  if(Preferences.writes!=0)throw new AssertionError("Opening a section changes preferences");
  if(((NavigationGlassSettingsActivity)editorFor("navigation")).mode!=0 || ((NavigationGlassSettingsActivity)editorFor("composer")).mode!=2 || ((NavigationGlassSettingsActivity)editorFor("glass")).mode!=1)throw new AssertionError("Wrong editor mode");
 }
}
class BaseFragment {}
class Preferences {static int writes;}
class NavigationGlassSettingsActivity extends BaseFragment {static final int NAVIGATION=0,SHARED=1,COMPOSER=2;int mode;NavigationGlassSettingsActivity(int mode){this.mode=mode;}}
class ChatHeaderSettingsActivity extends BaseFragment {}
class MessageSurfaceSettingsActivity extends BaseFragment {}
class GiftGlassSettingsActivity extends BaseFragment {}
class GlassDividerSettingsActivity extends BaseFragment {}
class StyleSurfacesSettingsActivity extends BaseFragment {static final int CHAT_LIST=0,CHAT=1;StyleSurfacesSettingsActivity(int mode){}}
class StyleEffectsSettingsActivity extends BaseFragment {static final int MOTION=0,SNOW=1;StyleEffectsSettingsActivity(int mode){}}
'''.replace('ROUTE',route))

    def test_compact_slider_keeps_units_at_both_endpoints_and_numeric_values(self):
        source = (JAVA / 'app/exteraless/appearance/AvatarCornersSeekBar.java').read_text(encoding='utf-8')
        formatter = method(source, '    public CharSequence getTextForHeader()')
        run_java('''
class PrivacyCheck {
 boolean compact;int roundedValue,min=0,max=100;String valueSuffix="%";
 Text leftTextView=new Text("Off"),rightTextView=new Text("Max");
 FORMAT
 public static void main(String[] args){
  PrivacyCheck p=new PrivacyCheck();p.compact=true;
  for(int value:new int[]{0,6,55,100}){p.roundedValue=value;if(!p.getTextForHeader().equals(value+" %"))throw new AssertionError("Missing units: "+p.getTextForHeader());}
  p.valueSuffix="dp";p.roundedValue=0;if(!p.getTextForHeader().equals("0 dp"))throw new AssertionError("Zero dp");
  p.compact=false;p.valueSuffix="";if(!p.getTextForHeader().equals("OFF"))throw new AssertionError("Legacy slider changed");
  p.roundedValue=100;if(!p.getTextForHeader().equals("MAX"))throw new AssertionError("Legacy max changed");
 }
}
class Text {String text;Text(String text){this.text=text;}String getText(){return text;}}
'''.replace('FORMAT',formatter))
