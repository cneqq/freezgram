import shutil,unittest
from pathlib import Path
from test_personal_privacy import method,run_java
JAVA=Path(__file__).resolve().parents[1]/'TMessagesProj/src/main/java'
@unittest.skipUnless(shutil.which('java'),'Java source launcher required')
class MessageSurfaceTests(unittest.TestCase):
 def test_profiles_are_independent_and_glass_respects_platform_and_power(self):
  source=(JAVA/'app/freezgram/MessageSurfaceStyle.java').read_text()
  helpers='\n'.join(method(source,'    public static '+signature) for signature in ['int settingsProfile(', 'int style(', 'int opacity(', 'int blur(', 'int intensity(', 'boolean glassEnabled('])
  run_java('''
class PrivacyCheck{
 HELPERS
 public static void main(String[] args){
  if(style(0)!=0||style(1)!=1||style(2)!=0||opacity(0)!=20||opacity(1)!=50||opacity(2)!=20||blur(0)!=3||blur(1)!=10||blur(2)!=3||intensity(0)!=10||intensity(1)!=50||intensity(2)!=10)throw new AssertionError("Profiles are coupled");
  AppearanceConfig.linked=true;
  if(style(1)!=style(0)||opacity(1)!=opacity(0)||blur(1)!=blur(0)||intensity(1)!=intensity(0)||settingsProfile(2)!=0)throw new AssertionError("Same settings failed to link all optical parameters");
  AppearanceConfig.linked=false;
  if(opacity(1)!=50||blur(1)!=10||style(1)!=1)throw new AssertionError("Unlink overwrote outgoing preferences");
  for(int sdk:new int[]{24,32,33,36})for(boolean blur:new boolean[]{false,true})for(boolean saver:new boolean[]{false,true}){
   Build.VERSION.SDK_INT=sdk;SharedConfig.blur=blur;LiteMode.saver=saver;
   if(glassEnabled(1)!=(sdk>=33&&blur&&!saver)||glassEnabled(0)||glassEnabled(2))throw new AssertionError("Glass fallback ignored");
  }
 }
}
class AppearanceConfig{
 static boolean linked;static boolean sameMessageSurfaces(){return linked;}
 static int incomingSurfaceStyle(){return 0;}static int outgoingSurfaceStyle(){return 1;}static int commentsSurfaceStyle(){return 0;}
 static int incomingSurfaceOpacity(){return 20;}static int outgoingSurfaceOpacity(){return 50;}static int commentsSurfaceOpacity(){return 80;}
 static int incomingSurfaceBlur(){return 3;}static int outgoingSurfaceBlur(){return 10;}static int commentsSurfaceBlur(){return 24;}
 static int incomingSurfaceIntensity(){return 10;}static int outgoingSurfaceIntensity(){return 50;}static int commentsSurfaceIntensity(){return 90;}
}
class Build{static class VERSION{static int SDK_INT;}}class SharedConfig{static boolean blur;static boolean chatBlurEnabled(){return blur;}}class LiteMode{static boolean saver;static boolean isGlassPowerSaverApplied(){return saver;}}
'''.replace('HELPERS',helpers))

 def test_preview_contains_real_incoming_outgoing_and_comment_post(self):
  source=(JAVA/'app/freezgram/MessageSurfacePreview.java').read_text()
  factory=method(source,'    static TLRPC.TL_message previewMessage(')
  run_java('''
class PrivacyCheck{
 FACTORY
 public static void main(String[] args){
  for(long self:new long[]{1,123456})for(int profile=0;profile<3;profile++){
   var m=previewMessage(self,profile);
   if(m.dialog_id==self||m.out!=(profile==1)||m.from_id.user_id!=(profile==1?self:m.dialog_id))throw new AssertionError("Preview incorrectly renders incoming as Saved Messages outgoing");
   if((m.replies!=null)!=(profile==2)||profile==2&&(!m.replies.comments||m.replies.replies!=12))throw new AssertionError("Native comments missing");
  }
 }
}
class TLRPC{
 static class Peer{long user_id;}static class TL_peerUser extends Peer{}
 static class TL_message{int id,date,flags;long dialog_id;boolean out;String message;Peer from_id,peer_id;Object media;TL_messageReplies replies;}
 static class TL_messageMediaEmpty{}static class TL_messageReplies{boolean comments;int replies;}
}
class LocaleController{static String getString(int n){return "Preview";}}class R{static class string{static int FreezgramPreviewIncomingText=1,FreezgramPreviewOutgoingText=2,FreezgramPreviewPostText=3;}}
'''.replace('FACTORY',factory))

 def test_native_shape_and_fallback_are_kept(self):
  source=(JAVA/'org/telegram/ui/Cells/ChatMessageCell.java').read_text()
  body=method(source,'    private void drawConfiguredMessageBackground(').replace('app.freezgram.MessageSurfaceStyle','Style')
  run_java('''
class PrivacyCheck{
 Message currentMessageObject=new Message();Object currentPosition,backgroundCacheParams;Delegate delegate=new Delegate();Shape currentBackgroundDrawable=new Shape();RectF messageSurfaceBounds=new RectF();Object messageSurface;
 BODY
 public static void main(String[] args){
  for(int style:new int[]{0,1})for(boolean success:new boolean[]{false,true})for(boolean grouped:new boolean[]{false,true}){
   var p=new PrivacyCheck();var c=new Canvas();Style.style=style;p.delegate.success=success;if(grouped)p.currentPosition=new Object();
   p.drawConfiguredMessageBackground(c,.5f);
   boolean attempt=style!=0&&!grouped;
   if(p.delegate.calls!=(attempt?1:0)||p.currentBackgroundDrawable.draws!=(attempt&&success?0:1)||c.depth!=0||c.clips!=(attempt?1:0))throw new AssertionError("Native bubble shape/fallback lost");
  }
 }
}
class Message{boolean isOutOwner(){return false;}}class Style{static int style;static int style(int p){return style;}}
class Delegate{boolean success;int calls;boolean drawMessageSurface(Object cell,Canvas c,RectF b,int profile,Object s,float alpha){calls++;if(alpha!=.5f)throw new AssertionError("Bubble animation alpha lost");return success;}}
class Shape{int draws;Object getBounds(){return null;}Object makePath(Object params){return null;}void drawCached(Canvas c,Object p){draws++;}}
class Canvas{int depth,clips;void save(){depth++;}void restore(){depth--;}void clipPath(Object p){clips++;}}class RectF{void set(Object r){}}
'''.replace('BODY',body))
