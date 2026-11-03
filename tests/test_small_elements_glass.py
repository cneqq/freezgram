"""Policies and snapshot lifetime for small app surfaces, with no real app data."""
import shutil
import unittest
from pathlib import Path
from test_personal_privacy import method, run_java

SOURCE = Path(__file__).resolve().parents[1] / 'TMessagesProj/src/main/java/app/freezgram/SmallElementsGlass.java'

@unittest.skipUnless(shutil.which('java'), 'Java source launcher required')
class SmallGlassTests(unittest.TestCase):
    def test_unsupported_devices_and_disabled_policy_keep_ordinary_surfaces(self):
        gate = method(SOURCE.read_text(), '    public static boolean isEnabled()')
        run_java('''
class PrivacyCheck {
 GATE
 public static void main(String[] args){
  for(int sdk:new int[]{24,31,32,33,36})for(boolean opt:new boolean[]{false,true})
  for(boolean blur:new boolean[]{false,true})for(boolean saver:new boolean[]{false,true}){
   Build.VERSION.SDK_INT=sdk;AppearanceConfig.opt=opt;SharedConfig.blur=blur;LiteMode.saver=saver;
   if(isEnabled()!=(sdk>=33&&opt&&blur&&!saver))throw new AssertionError("Unsafe glass activation");
  }
 }
}
class Build{static class VERSION{static int SDK_INT;}}
class AppearanceConfig{static boolean opt;static boolean smallElementsGlass(){return opt;}}
class SharedConfig{static boolean blur;static boolean chatBlurEnabled(){return blur;}}
class LiteMode{static boolean saver;static boolean isGlassPowerSaverApplied(){return saver;}}
'''.replace('GATE', gate))

    def test_message_menu_opt_in_is_not_overridden_by_legacy_gate(self):
        # SOURCE is .../java/app/freezgram/SmallElementsGlass.java.
        path=SOURCE.parent.parent/'exteraless/appearance/GlassMenuHelper.java'
        gate=method(path.read_text(),'    public static boolean isEnabled(int currentAccount, Theme.ResourcesProvider resourcesProvider)').replace('app.freezgram.SmallElementsGlass','SmallElementsGlass')
        run_java('''
class PrivacyCheck{
 GATE
 public static void main(String[] args){
  for(boolean small:new boolean[]{false,true})for(boolean old:new boolean[]{false,true})for(boolean allowed:new boolean[]{false,true}){
   SmallElementsGlass.enabled=small;AppearanceConfig.old=old;BlurredBackgroundProviderImpl.allowed=allowed;
   if(isEnabled(0,null)!=(small||(old&&allowed)))throw new AssertionError("Explicit glass choice suppressed by legacy flags");
  }
 }
}
class SmallElementsGlass{static boolean enabled;static boolean isEnabled(){return enabled;}}
class AppearanceConfig{static boolean old;static boolean glassMessageMenu(){return old;}}
class Theme{static class ResourcesProvider{}}
class BlurredBackgroundProviderImpl{static boolean allowed;static boolean checkBlurEnabled(int account,Theme.ResourcesProvider provider){return allowed;}}
'''.replace('GATE',gate))

    def test_snapshot_bridge_updates_when_bitmap_or_settings_change(self):
        source=SOURCE.read_text()
        start=source.index('    private static final class SnapshotSource')
        end=source.index('    public static final class NotificationBackdrop',start)
        nested=source[start:end]
        run_java('''
class PrivacyCheck {
 NESTED
 public static void main(String[] args){
  var input=new BlurredBackgroundSourceBitmap();var bridge=new SnapshotSource(input);var c=new Canvas();
  bridge.draw(c,0,0,100,50);var retained=c.nodes.get(0);bridge.draw(c,0,0,100,50);
  if(input.calls!=1)throw new AssertionError("Repeated static capture");
  input.bitmap=new Bitmap();bridge.draw(c,0,0,100,50);
  if(input.calls!=2)throw new AssertionError("New menu image ignored");
  LiquidGlassStyle.rev++;bridge.draw(c,0,0,100,50);
  if(input.calls!=3)throw new AssertionError("Shared optics ignored");
  bridge.draw(c,0,0,200,50);
  if(input.calls!=4)throw new AssertionError("Resized capture ignored");
  if(retained.w!=228||retained.h!=178||c.nodes.get(c.nodes.size()-1)==retained)
   throw new AssertionError("A later crop resized a wallpaper node already retained by the canvas");
  var resized=c.nodes.get(c.nodes.size()-1);bridge.draw(c,50,0,250,50);
  if(c.nodes.get(c.nodes.size()-1)==resized)throw new AssertionError("Moving crop rewrote the old origin");
  var moved=c.nodes.get(c.nodes.size()-1);bridge.draw(c,50,0,250,50);
  if(c.nodes.get(c.nodes.size()-1)!=moved)throw new AssertionError("Unchanged crop should reuse its recording");
  var one=bridge.createDrawable();var two=bridge.createDrawable();
  if(one.source==two.source)throw new AssertionError("Reaction shapes must not share mutable recordings");
  var own=(SnapshotSource)one.source;own.draw(c,1000,2000,1100,2050);
  if(c.nodes.get(c.nodes.size()-1).w!=228||c.nodes.get(c.nodes.size()-1).h!=178)throw new AssertionError("Capture should be cropped around the target, not the entire screen");
  var gift=new SnapshotSource(input,true);gift.draw(c,0,0,100,50);int calls=input.calls;
  if(c.nodes.get(c.nodes.size()-1).blur!=24)throw new AssertionError("Gift used shared blur");
  LiquidGlassStyle.rev++;gift.draw(c,0,0,100,50);
  if(input.calls!=calls)throw new AssertionError("Shared settings altered gift profile");
  GiftGlassStyle.rev++;gift.draw(c,0,0,100,50);
  if(input.calls!=calls+1||!((SnapshotSource)gift.createDrawable().source).gift)throw new AssertionError("Gift settings or cloned profile lost");
 }
}
class Canvas{java.util.ArrayList<BlurredBackgroundSourceRenderNode> nodes=new java.util.ArrayList<>();boolean isHardwareAccelerated(){return true;}void translate(float x,float y){}void save(){}void restore(){}}class Bitmap{}
class BlurredBackgroundDrawable{BlurredBackgroundSource source;}
class BlurredBackgroundDrawableRenderNode extends BlurredBackgroundDrawable{BlurredBackgroundDrawableRenderNode(BlurredBackgroundSource source){this.source=source;}}
interface BlurredBackgroundSource{void draw(Canvas c,float l,float t,float r,float b);}
class BlurredBackgroundSourceBitmap implements BlurredBackgroundSource{
 Bitmap bitmap=new Bitmap();int calls;Bitmap getBitmap(){return bitmap;}
 public void draw(Canvas c,float l,float t,float r,float b){calls++;}
}
class BlurredBackgroundSourceRenderNode implements BlurredBackgroundSource{
 float blur;int w,h;boolean recorded;BlurredBackgroundSourceRenderNode(BlurredBackgroundSource fallback){}
 BlurredBackgroundDrawable createDrawable(){return new BlurredBackgroundDrawable();}
 boolean needUpdateDisplayList(int width,int height){return !recorded||w!=width||h!=height;}
 Canvas beginRecording(int width,int height){w=width;h=height;return new Canvas();}void endRecording(){recorded=true;}
 void setBlur(float radius,Object effect){blur=radius;}public void draw(Canvas c,float l,float t,float r,float b){c.nodes.add(this);}
}
class GiftGlassStyle{static int rev=99;static int revision(){return rev;}static int blurRadiusDp(){return 24;}}
class LiquidGlassStyle{static int rev;static int revision(){return rev;}static int blurRadiusDp(){return 6;}static boolean enhanceColors(){return true;}}
class AndroidUtilities{static int dp(int n){return n;}static float dpf2(int n){return n;}}
class RenderNodeEffects{static Object getSaturationX3RenderEffect(){return null;}}
'''.replace('NESTED',nested).replace('org.telegram.messenger.utils.RenderNodeEffects','RenderNodeEffects'))

    def test_notice_release_drops_capture_and_render_references(self):
        release = method(SOURCE.read_text(), '        public void release()')
        # method() is scoped to four spaces; the selected function is eight spaces deep.
        release=release[:release.index('\n        }')+len('\n        }')]
        run_java('''
class PrivacyCheck{
 Object glass=new Object(),bitmap=new Object(),capturedRoot=new Object(),capturedOwner=new Object(),scrollListener;boolean captureDirty=true;ViewTreeObserver scrollObserver;Source bitmapSource=new Source();
 RELEASE
 public static void main(String[] args){
  var p=new PrivacyCheck();p.release();p.release();
  if(p.glass!=null||p.bitmap!=null||p.capturedRoot!=null||p.bitmapSource.value!=null)throw new AssertionError("Capture retained");
 }
}
class Source{Object value=new Object();void setBitmap(Object o){value=o;}}
class ViewTreeObserver{boolean isAlive(){return true;}void removeOnScrollChangedListener(Object o){}}
'''.replace('RELEASE',release))

    def test_reaction_panel_and_bubbles_keep_independent_retained_drawables(self):
        path=SOURCE.parents[2]/'org/telegram/ui/Components/ReactionsContainerLayout.java'
        configure=method(path.read_text(),'    public void setGlassBackground(')
        run_java('''
class PrivacyCheck{
 BlurredBackgroundDrawable glassBackground,glassBigBubbleBackground,glassSmallBubbleBackground;
 BlurredBackgroundProvider glassBackgroundColorProvider;BlurredBackgroundDrawableViewFactory glassBackgroundFactory;
 void invalidate(){}
 CONFIGURE
 public static void main(String[] args){
  var p=new PrivacyCheck();p.setGlassBackground(new BlurredBackgroundDrawableViewFactory(),new BlurredBackgroundProvider());
  p.glassBackground.size=300;p.glassBigBubbleBackground.size=30;p.glassSmallBubbleBackground.size=12;
  if(p.glassBackground.size!=300||p.glassBigBubbleBackground.size!=30||p.glassSmallBubbleBackground.size!=12)
   throw new AssertionError("A bubble overwrote the retained main-panel drawing");
 }
}
class BlurredBackgroundDrawable{int size;BlurredBackgroundDrawable setColorProvider(BlurredBackgroundProvider p){return this;}}
class BlurredBackgroundProvider{}
class BlurredBackgroundDrawableViewFactory{BlurredBackgroundDrawable create(Object v,boolean popup){return new BlurredBackgroundDrawable();}}
class SmallElementsGlass{static boolean isEnabled(){return true;}}
class LiquidGlassStyle{static void configure(BlurredBackgroundDrawable d){}}
'''.replace('CONFIGURE',configure).replace('app.freezgram.',''))
