import shutil
import unittest
from pathlib import Path
from test_personal_privacy import method, run_java

JAVA = Path(__file__).resolve().parents[1] / 'TMessagesProj/src/main/java'

@unittest.skipUnless(shutil.which('java'), 'Java source launcher required')
class StylePreviewFixes(unittest.TestCase):
    def test_previews_start_visible_and_can_be_collapsed(self):
        source=(JAVA/'app/freezgram/StyleEditorUi.java').read_text(encoding='utf-8')
        preview=method(source,'    static void preview(')
        run_java('''
class PrivacyCheck {
 static LinearLayout section(LinearLayout content,int title,Theme.ResourcesProvider provider){return new LinearLayout();}
 PREVIEW
 public static void main(String[] args){
  View sample=new View();sample.visibility=View.GONE;preview(new LinearLayout(),sample,240,null);
  if(sample.visibility!=View.VISIBLE)throw new AssertionError("Preview starts hidden");
  TextSettingsCell.last.click();if(sample.visibility!=View.GONE)throw new AssertionError("Collapse broken");
  TextSettingsCell.last.click();if(sample.visibility!=View.VISIBLE)throw new AssertionError("Expand broken");
 }
}
class View{static int GONE=8,VISIBLE=0;int visibility;void setVisibility(int v){visibility=v;}int getVisibility(){return visibility;}}
class LinearLayout{Object getContext(){return null;}void addView(Object v){}void addView(Object v,Object lp){}}
class TextSettingsCell{static TextSettingsCell last;interface Listener{void click(View v);}Listener listener;TextSettingsCell(Object c,Object p){last=this;}void setBackground(Object d){}void setTextAndValue(String a,String b,boolean c){}void setOnClickListener(Listener l){listener=l;}void click(){listener.click(null);}}
class Theme{static class ResourcesProvider{}static Object getSelectorDrawable(boolean b){return null;}}
class LayoutHelper{static int MATCH_PARENT=-1;static Object createLinear(int a,int b){return null;}}
class LocaleController{static String getString(int n){return "Label";}}
class R{static class string{static int FreezgramStylePreview,FreezgramStyleExpand,FreezgramStyleCollapse;}}
'''.replace('PREVIEW',preview))

    def test_header_refresh_preserves_the_native_wallpaper_child(self):
        source=(JAVA/'app/freezgram/ChatHeaderPreview.java').read_text(encoding='utf-8')
        refresh=method(source,'    void refresh()')
        run_java('''
class PrivacyCheck {
 Object backgroundView=new Object(),provider,owner;int account;
 ChatAvatarContainer avatar;ActionBar bar;Factory factory=new Factory();
 java.util.ArrayList<Object> children=new java.util.ArrayList<>();
 Object getContext(){return null;}void invalidate(){}
 void removeAllViews(){children.clear();}void removeView(Object view){children.remove(view);}
 void addView(Object view,Object lp){children.add(view);}
 REFRESH
 public static void main(String[] args){
  PrivacyCheck p=new PrivacyCheck();p.children.add(p.backgroundView);
  for(boolean ios:new boolean[]{true,false,true,false}){
   AppearanceConfig.ios=ios;p.refresh();
   if(p.children.size()!=2||p.children.get(0)!=p.backgroundView||p.children.get(1)!=p.bar)throw new AssertionError("Header refresh removed/duplicated wallpaper");
  }
 }
}
class Factory{void setLiquidGlassEffectAllowed(boolean b){}}
class ActionBar {
 ActionBar(Object context,Object provider){}void setOccupyStatusBar(boolean b){}void setBackButtonImage(int r){}
 void setItemsColor(int color,boolean actionMode){}void addView(Object view,Object lp){}
 void setupGlass(Object factory,Object colors){}void setIosChatHeaderGlass(boolean b){}
 void setChatAvatarContainer(Object c){}void setForcedMenuWidth(int w){}Menu createMenu(){return new Menu();}
}
class Menu{void addItem(int id,int icon){}}
class ChatAvatarContainer {
 ChatAvatarContainer(Object c,Object owner,boolean b,Object p){}protected boolean isCentered(){return false;}
 void onDestroy(){}void setOccupyStatusBar(boolean b){}void setTitle(String t){}void setSubtitle(String t){}
 void setUserAvatar(Object user,boolean self){}void setActionBar(Object bar){}
}
class AppearanceConfig{static boolean ios;static boolean iosChatHeader(){return ios;}}
class ChatHeaderUiHelper{static void setupGlassAvatarContainer(Object a){}static void applyChatHeaderGlassStyle(Object a){}}
class UserConfig{static UserConfig getInstance(int account){return new UserConfig();}Object getCurrentUser(){return new Object();}}
class UserObject{static String getUserName(Object user){return "User";}}
class IosLiquidGlassChatHeader{static boolean isEnabled(){return true;}static Object colors(Object p){return null;}}
class BlurredBackgroundProviderImpl{static Object topPanelChatActivity(Object p){return null;}}
class AndroidUtilities{static int dp(int v){return v;}}
class LayoutHelper{static int MATCH_PARENT=-1,WRAP_CONTENT=-2;static Object createFrame(int... args){return null;}}
class Gravity{static int TOP,LEFT;}
class Theme{static int key_chat_topPanelTitle;static int getColor(int k,Object p){return 0;}}
class LocaleController{static String getString(int n){return "Online";}}
class R{static class string{static int Online;}static class drawable{static int call,ic_ab_back,ic_ab_other;}}
'''.replace('REFRESH',refresh))

    def test_preview_material_follows_the_surface_switch(self):
        source=(JAVA/'app/freezgram/NavigationGlassSettingsActivity.java').read_text(encoding='utf-8')
        gate=method(source,'    public static boolean previewMaterialRequested(int mode)')
        run_java('''
class PrivacyCheck {
 static final int NAVIGATION=0,SHARED=1,COMPOSER=2;
 GATE
 public static void main(String[] args){
  for(boolean ios:new boolean[]{false,true})for(boolean nav:new boolean[]{false,true})for(boolean composer:new boolean[]{false,true}){
   MainTabsUiHelper.ios=ios;AppearanceConfig.nav=nav;ChatComposerGlass.enabled=composer;
   if(previewMaterialRequested(0)!=(ios&&nav)||previewMaterialRequested(2)!=composer||!previewMaterialRequested(1))throw new AssertionError("Preview ignores surface choice");
  }
 }
}
class MainTabsUiHelper{static boolean ios;static boolean isIosNavigationBar(){return ios;}}
class AppearanceConfig{static boolean nav;static boolean iosLiquidGlassNavigation(){return nav;}}
class ChatComposerGlass{static boolean enabled;static boolean isRequested(){return enabled;}}
'''.replace('GATE',gate))

    def test_composer_glass_migrates_legacy_values_and_remains_independent(self):
        source=(JAVA/'app/freezgram/ChatComposerGlass.java').read_text(encoding='utf-8')
        resolve=method(source,'    public static boolean resolveGlassPreference(')
        requested=method(source,'    public static boolean isRequested()')
        run_java('''
class PrivacyCheck {
 RESOLVE
 REQUESTED
 public static void main(String[] args){
  for(int legacy:new int[]{0,1,2})for(boolean value:new boolean[]{false,true}){
   if(resolveGlassPreference(legacy,false,value)!=(legacy!=0))throw new AssertionError("Legacy glass selection lost");
   if(resolveGlassPreference(legacy,true,value)!=value)throw new AssertionError("Layout overrides explicit glass switch");
   AppearanceConfig.style=legacy;AppearanceConfig.enabled=value;
   if(isRequested()!=value)throw new AssertionError("Composer ignores explicit switch");
  }
 }
}
class AppearanceConfig{static int style;static boolean enabled;static boolean composerLiquidGlass(){return enabled;}}
'''.replace('RESOLVE',resolve).replace('REQUESTED',requested))

    def test_dividers_never_precede_the_material_or_either_row(self):
        source=(JAVA/'app/freezgram/GlassDividers.java').read_text(encoding='utf-8')
        alpha=method(source,'    public static float popupVisibility(')
        run_java('''
class PrivacyCheck {
 ALPHA
 public static void main(String[] args){
  for(int material:new int[]{0,32,128,255})for(float a:new float[]{0,.25f,1})for(float b:new float[]{0,.5f,1}){
   float v=popupVisibility(material,a,b);
   if(v<0||v>material/255f||v>a||v>b)throw new AssertionError("Separator appears before its backing/rows");
  }
  if(popupVisibility(255,1,1)!=1)throw new AssertionError("Completed menu lost separators");
 }
}
'''.replace('ALPHA',alpha))
