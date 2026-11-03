import shutil,unittest
from pathlib import Path
from test_personal_privacy import method,run_java
JAVA=Path(__file__).resolve().parents[1]/'TMessagesProj/src/main/java'
@unittest.skipUnless(shutil.which('java'),'Java source launcher required')
class HeaderBadgeTests(unittest.TestCase):
 def test_badge_survives_premium_centered_header_and_muted_icon_updates(self):
  source=(JAVA/'org/telegram/ui/Components/ChatAvatarContainer.java').read_text()
  a=source.index('            titleTextView.setRightDrawable(emojiStatusDrawable);',source.index('    public void setTitle(CharSequence value, boolean scam'))
  b=source.index('        } else {\n            titleTextView.setRightDrawable(null);',a)
  branch=source[a:b]
  icons=method(source,'    public void setTitleIcons(')
  run_java('''
class PrivacyCheck{
 Text titleTextView=new Text();boolean rightDrawableIsScamOrVerified,rightDrawableIsScam,centered;String rightDrawableContentDescription,rightDrawable2ContentDescription;Drawable emojiStatusDrawable=new Drawable();
 boolean isCentered(){return centered;}String getString(int n){return "description";}void checkActionBar(boolean a){}
 void premium(){BRANCH}
 ICONS
 public static void main(String[] args){
  for(boolean centered:new boolean[]{false,true})for(boolean muted:new boolean[]{false,true}){
   var p=new PrivacyCheck();p.centered=centered;var badge=new FreezgramBadgeDrawable();p.titleTextView.second=badge;p.rightDrawableIsScamOrVerified=true;
   p.premium();p.setTitleIcons(null,muted?new Drawable():null);
   if(p.titleTextView.second!=badge||!p.rightDrawableIsScamOrVerified||p.titleTextView.first!=p.emojiStatusDrawable)throw new AssertionError("Community plane lost to premium or mute icon");
  }
  var p=new PrivacyCheck();p.centered=true;p.titleTextView.second=new Drawable();p.premium();
  if(p.titleTextView.second!=null)throw new AssertionError("Ordinary centered header changed");
 }
}
class Drawable{}class FreezgramBadgeDrawable extends Drawable{}
class Text{Drawable first,second;void setRightDrawable(Drawable d){first=d;}Drawable getRightDrawable2(){return second;}void setRightDrawable2(Drawable d){second=d;}void setLeftDrawable(Drawable d){}}
class R{static class string{static int AccDescrPremium=1,NotificationsMuted=2;}}
'''.replace('BRANCH',branch).replace('ICONS',icons))
