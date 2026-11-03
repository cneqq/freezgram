import shutil,unittest
from pathlib import Path
from test_personal_privacy import method,run_java
JAVA=Path(__file__).resolve().parents[1]/'TMessagesProj/src/main/java'
@unittest.skipUnless(shutil.which('java'),'Java source launcher required')
class StyleLinkTests(unittest.TestCase):
 def test_copied_links_have_freezgram_path_and_no_scheme(self):
  source=(JAVA/'app/freezgram/StyleSettingsLinks.java').read_text()
  run_java('''
class PrivacyCheck{
 LINK
 public static void main(String[] args){
  for(String section:new String[]{null,"messages","gifts","navigation","header","composer","glass","chatList","surfaces","dividers"}){
   String result=link(section);
   if(!result.startsWith("t.me/freezgram/style")||result.contains("://")||result.contains("exteraless"))throw new AssertionError("Wrong copy-link format");
   var uri=java.net.URI.create("https://"+result);
   if(!uri.getPath().equals("/freezgram/style")||(section==null?uri.getQuery()!=null:!uri.getQuery().equals("r="+section)))throw new AssertionError("Link lost target");
  }
 }
}
'''.replace('LINK',method(source,'    static String link(')))

 def test_opening_link_opens_editor_or_expands_choice_without_applying_it(self):
  source=(JAVA/'app/freezgram/FreezgramStyleActivity.java').read_text()
  route=method(source,'    public static BaseFragment screenForLink(')
  run_java('''
class PrivacyCheck{
 public static void main(String[] args){
  if(FreezgramStyleActivity.screenForLink("messages") instanceof FreezgramStyleActivity)throw new AssertionError("Editor link only opens parent list");
  for(String key:new String[]{null,"motion","motionStandard","snow","snowDots"}){
   var root=(FreezgramStyleActivity)FreezgramStyleActivity.screenForLink(key);
   if(root.motionExpanded!=(key!=null&&key.startsWith("motion"))||root.snowExpanded!=(key!=null&&key.startsWith("snow")))throw new AssertionError("Linked choice not revealed");
  }
 }
}
class BaseFragment{}
class FreezgramStyleActivity extends BaseFragment{
 boolean motionExpanded,snowExpanded;
 static BaseFragment editorFor(String key){return "messages".equals(key)?new BaseFragment():null;}
 ROUTE
}
'''.replace('ROUTE',route))
