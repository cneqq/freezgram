import shutil,unittest
from pathlib import Path
from test_personal_privacy import run_java
@unittest.skipUnless(shutil.which('java'),'Java source launcher required')
class NoticeScrollTests(unittest.TestCase):
 def test_scroll_refreshes_snapshot_without_recreating_material_and_releases_listener(self):
  path=Path(__file__).resolve().parents[1]/'TMessagesProj/src/main/java/app/freezgram/SmallElementsGlass.java'
  source=path.read_text();a=source.index('    public static final class NotificationBackdrop');nested=source[a:source.rfind('\n}')]
  run_java('''
import java.lang.ref.WeakReference;
class PrivacyCheck{
 static boolean isEnabled(){return SmallElementsGlass.enabled;}static Factory createSource(Object o){return new Factory();}
 NESTED
 public static void main(String[] args){
  var backdrop=new NotificationBackdrop();var root=new View();var owner=new View();owner.root=root;var canvas=new Canvas();
  if(!backdrop.draw(canvas,owner,new Rect(),null,16)||AndroidUtilities.captures!=1||root.observer.listeners.size()!=1)throw new AssertionError("Initial capture/listener missing");
  Object oldBitmap=backdrop.bitmap,material=backdrop.glass;
  backdrop.draw(canvas,owner,new Rect(),null,16);if(AndroidUtilities.captures!=1)throw new AssertionError("Idle notice recaptures needlessly");
  root.observer.listeners.get(0).onScrollChanged();
  if(owner.invalidates!=1)throw new AssertionError("Scroll does not request a new notice frame");
  backdrop.draw(canvas,owner,new Rect(),null,16);
  if(AndroidUtilities.captures!=2||backdrop.bitmap==oldBitmap||backdrop.glass!=material)throw new AssertionError("Frozen scroll backdrop or replaced material");
  canvas.hardware=false;if(backdrop.draw(canvas,owner,new Rect(),null,16)||AndroidUtilities.captures!=2)throw new AssertionError("Software fallback ignored");canvas.hardware=true;
  AndroidUtilities.makingGlobalBlurBitmap=true;if(backdrop.draw(canvas,owner,new Rect(),null,16))throw new AssertionError("Notice captures itself");AndroidUtilities.makingGlobalBlurBitmap=false;
  root.width=600;backdrop.draw(canvas,owner,new Rect(),null,16);
  if(root.observer.listeners.size()!=1||AndroidUtilities.captures!=3)throw new AssertionError("Resize duplicated scroll listener");
  backdrop.release();backdrop.release();
  if(!root.observer.listeners.isEmpty()||backdrop.bitmap!=null||backdrop.capturedOwner!=null||backdrop.glass!=null)throw new AssertionError("Listener/capture leaked after detach");
 }
}
class Canvas{boolean hardware=true;boolean isHardwareAccelerated(){return hardware;}}class Rect{}class Bitmap{}
class View{View root;int width=400,invalidates;ViewTreeObserver observer=new ViewTreeObserver();View getRootView(){return root==null?this:root;}int getWidth(){return width;}int getHeight(){return 800;}ViewTreeObserver getViewTreeObserver(){return observer;}void invalidate(){invalidates++;}void getLocationOnScreen(int[] a){a[0]=a[1]=0;}}
class ViewTreeObserver{interface OnScrollChangedListener{void onScrollChanged();}java.util.ArrayList<OnScrollChangedListener> listeners=new java.util.ArrayList<>();void addOnScrollChangedListener(OnScrollChangedListener l){listeners.add(l);}void removeOnScrollChangedListener(OnScrollChangedListener l){listeners.remove(l);}boolean isAlive(){return true;}}
class BlurredBackgroundSourceBitmap{Bitmap value;void setBitmap(Bitmap b){value=b;}}
class BlurredBackgroundDrawable{void setColorProvider(Object o){}void setSourceOffset(int x,int y){}void setBounds(Rect r){}void setRadius(int n){}void updateColors(){}void draw(Canvas c){}}
class BlurredBackgroundDrawableRenderNode extends BlurredBackgroundDrawable{void setLiquidGlassEffectAllowed(boolean b){}}
class Factory{BlurredBackgroundDrawable createDrawable(){return new BlurredBackgroundDrawableRenderNode();}}
class LiquidGlassStyle{static Object colors(Object p,java.util.function.BooleanSupplier e){return null;}static void configure(Object d){}}
class SmallElementsGlass{static boolean enabled=true;static boolean isEnabled(){return enabled;}}
class AndroidUtilities{static boolean makingGlobalBlurBitmap;static int captures;static int dp(int n){return n;}static void makeGlobalBlurBitmap(java.util.function.Consumer<Bitmap> callback,float scale,int radius,View root,Object excluded){captures++;callback.accept(new Bitmap());}}
class Blur3Utils{static void checkBitmapSourceMatrixScale(Object source,View root){}}
class Theme{static class ResourcesProvider{}}
'''.replace('NESTED',nested))
