import shutil,unittest
from pathlib import Path
from test_personal_privacy import method,run_java
JAVA=Path(__file__).resolve().parents[1]/'TMessagesProj/src/main/java'
@unittest.skipUnless(shutil.which('java'),'Java source launcher required')
class StyleCleanupTests(unittest.TestCase):
 def test_new_settings_links_and_legacy_aliases(self):
  source=(JAVA/'tw/nekomimi/nekogram/helpers/SettingsHelper.java').read_text()
  helpers=method(source,'    public static boolean isDeepLink(')+method(source,'    public static String linkPathFor(')
  run_java('''
class PrivacyCheck{
 static String HOST_NAGRAM="nasettings",HOST_EXTERALESS="freezgram";
 static java.util.Set<String> EXTERALESS_SCREENS=java.util.Set.of("settings","appearance","general","chats");
 HELPERS
 public static void main(String[] args){
  if(!linkPathFor("exteraless_appearance").equals("freezgram/appearance"))throw new AssertionError("Copied settings links retain old host");
  if(!isDeepLink("freezgram/appearance")||!isDeepLink("exteraless/appearance")||isDeepLink("freezgram/not-a-setting")||isDeepLink(null))throw new AssertionError("Deep-link compatibility");
 }
}
'''.replace('HELPERS',helpers))

 def test_preview_tab_respects_allocated_width_without_changing_real_attachment_tabs(self):
  source=(JAVA/'org/telegram/ui/Components/glass/GlassTabView.java').read_text()
  methods=method(source,'    public void setUseParentWidth(')+method(source,'    protected void onMeasure(')
  run_java('''
class PrivacyCheck extends Base{
 boolean selfMeasure=true;int additionalWidth;float measureAttachTabWidth(){return 80;}void requestLayout(){}
 METHODS
 public static void main(String[] args){
  var p=new PrivacyCheck();p.onMeasure(160,48);if(p.width!=80)throw new AssertionError("Native attachment width changed");
  p.setUseParentWidth(true);p.onMeasure(160,48);if(p.width!=160)throw new AssertionError("Preview ignores layout weights");
 }
}
class Base{int width;protected void onMeasure(int w,int h){width=w;}}
class MeasureSpec{static int EXACTLY=1;static int makeMeasureSpec(int w,int mode){return w;}}
'''.replace('METHODS',methods))
