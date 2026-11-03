"""Production action-cell glass: separate recordings, fallback and cleanup."""
import shutil,unittest
from pathlib import Path
from test_personal_privacy import method,run_java
JAVA=Path(__file__).resolve().parents[1]/'TMessagesProj/src/main/java'

@unittest.skipUnless(shutil.which('java'),'Java source launcher required')
class ServiceGlassTests(unittest.TestCase):
 def test_service_and_gift_materials_do_not_share_geometry_and_keep_fallback(self):
  source=(JAVA/'org/telegram/ui/Cells/ChatActionCell.java').read_text()
  methods='\n'.join(method(source,s) for s in ['    public void setServiceGlassFactory(', '    private void releaseServiceGlass()', '    private boolean drawServiceGlass('])
  aliases={'org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory':'Factory','org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable':'Drawable','app.freezgram.GiftGlassStyle':'GiftGlassStyle','app.freezgram.LiquidGlassStyle':'LiquidGlassStyle','app.freezgram.SmallElementsGlass':'SmallElementsGlass'}
  for a,b in aliases.items():methods=methods.replace(a,b)
  run_java('''
class PrivacyCheck{
 Factory serviceGlassFactory=new Factory();Drawable[] serviceGlass=new Drawable[2];RectF serviceGlassBounds=new RectF();Object themeDelegate;
 GiftGlassStyle.SoftCard softGiftBackground=new GiftGlassStyle.SoftCard();
 int overrideBackground=-1;MessageObject currentMessageObject=new MessageObject();Star starGiftLayout=new Star();boolean floating;
 float getAlpha(){return .5f;}boolean isFloating(){return floating;}
 METHODS
 static int dp(int n){return n;}
 Object getThemedPaint(Object key){return null;}boolean hasGradientService(){return true;}
 public static void main(String[] args){
  var p=new PrivacyCheck();var c=new Canvas();Path text=new Path();
  if(!p.drawServiceGlass(c,text,null,11,false,true))throw new AssertionError("Date/service missing");
  Drawable service=p.serviceGlass[0];RectF card=new RectF();card.right=200;card.bottom=220;
  if(!p.drawServiceGlass(c,null,card,20,true,true))throw new AssertionError("Gift missing");
  if(p.serviceGlass[0]==p.serviceGlass[1]||service.right!=100||p.serviceGlass[1].right!=200||service.alpha!=127||c.clips!=1)throw new AssertionError("Shared/misaligned recordings");
  if(!p.starGiftLayout.glass)throw new AssertionError("Unique-gift fill still covers glass");
  p.starGiftLayout.colored=true;p.serviceGlass[1]=null;
  if(!p.drawServiceGlass(c,null,card,20,true,true)||p.serviceGlass[1]!=null||!p.starGiftLayout.glass)throw new AssertionError("Collectible replaced with wallpaper glass");
  p.starGiftLayout.colored=false;p.drawServiceGlass(c,null,card,20,true,true);
  p.overrideBackground=1;if(p.drawServiceGlass(c,text,null,11,false,true))throw new AssertionError("Explicit background overwritten");p.overrideBackground=-1;
  c.hardware=false;if(p.drawServiceGlass(c,text,null,11,false,true))throw new AssertionError("Software path has no fallback");c.hardware=true;
  SmallElementsGlass.enabled=false;if(p.drawServiceGlass(c,text,null,11,false,true))throw new AssertionError("Small opt-out ignored");
  if(!p.drawServiceGlass(c,null,card,20,true,true))throw new AssertionError("Independent gift toggle suppressed");
  SmallElementsGlass.gifts=false;if(p.drawServiceGlass(c,null,card,20,true,true))throw new AssertionError("Gift opt-out ignored");
  SmallElementsGlass.gifts=true;p.currentMessageObject.star=false;p.currentMessageObject.type=-1;
  if(p.drawServiceGlass(c,null,card,20,true,true))throw new AssertionError("Unrelated wallet/action card changed");
  p.currentMessageObject.star=true;GiftGlassStyle.selected=2;p.starGiftLayout=null;p.serviceGlassFactory=null;c.hardware=false;
  if(!p.drawServiceGlass(c,null,card,20,true,true)||p.softGiftBackground.calls!=1)throw new AssertionError("Soft background depends on glass hardware or factory");
  p.currentMessageObject.star=false;p.currentMessageObject.type=-1;
  if(p.drawServiceGlass(c,null,card,20,true,true))throw new AssertionError("Soft style changed unrelated card");
  GiftGlassStyle.selected=1;
  // Greeting/business-intro hints are custom text cells without a MessageObject.
  p.currentMessageObject=null;SmallElementsGlass.enabled=true;c.hardware=true;
  if(p.drawServiceGlass(c,text,null,11,false,false))throw new AssertionError("Missing source must keep native greeting background");
  p.serviceGlassFactory=new Factory();
  if(!p.drawServiceGlass(c,text,null,11,false,false)||p.serviceGlass[0].alpha!=255)throw new AssertionError("Standalone greeting did not render clipped glass");
  Drawable gift=p.serviceGlass[1];p.setServiceGlassFactory(new Factory());
  if(service.callback!=null||gift.callback!=null||p.serviceGlass[0]!=null||p.serviceGlass[1]!=null)throw new AssertionError("Old recording retained");
 }
}
class Canvas{boolean hardware=true;int clips;boolean isHardwareAccelerated(){return hardware;}void save(){}void restore(){}void clipPath(Path p){clips++;}}
class Path{void computeBounds(RectF r,boolean exact){r.left=0;r.top=0;r.right=100;r.bottom=24;}}
class RectF{float left,top,right,bottom;void set(RectF r){left=r.left;top=r.top;right=r.right;bottom=r.bottom;}boolean isEmpty(){return right<=left||bottom<=top;}}
class Factory{Object getSource(){return null;}Factory withSource(Object s){return this;}Drawable create(Object cell,Object colors){return new Drawable();}}
class Drawable{int right,alpha;Object callback;void setCallback(Object o){callback=o;}void setBounds(int l,int t,int r,int b){right=r;}void setRadius(float r){}void setAlpha(int a){alpha=a;}void updateColors(){}void draw(Canvas c){}}
class Star{boolean glass,colored;boolean hasColoredBackdrop(){return colored;}void setGlassBackground(boolean b){glass=b;}}
class SmallElementsGlass{static boolean enabled=true,gifts=true;static boolean isEnabled(){return enabled;}static boolean areServiceBubblesEnabled(){return enabled;}static boolean areGiftCardsEnabled(){return gifts;}static Object createGiftSource(Object s){return s;}}
class Theme{static Object key_paint_chatActionBackground,key_paint_chatActionBackgroundDarken;}
class GiftGlassStyle{static int ORDINARY=0,SOFT=2,selected=1;static boolean customOrdinary(){return false;}static void drawOrdinary(Canvas c,RectF r,float f,Object p,Object d){}static int style(){return selected;}static class SoftCard{int calls;void draw(Canvas c,RectF r,float radius,Object p,float a,boolean o){calls++;}}static Object colors(Object p){return null;}static void configure(Drawable d){}}
class LiquidGlassStyle{static Object surfaceColors(Object p,java.util.function.BooleanSupplier e){return null;}static void configure(Drawable d){}}
class MessageObject{static int TYPE_GIFT_PREMIUM=1,TYPE_GIFT_PREMIUM_CHANNEL=2,TYPE_GIFT_STARS=3,TYPE_GIFT_OFFER=4;boolean star=true;int type;boolean isStarGiftAction(){return star;}}
'''.replace('METHODS',methods))

 def test_collectible_keeps_opaque_background_and_glass_finish(self):
  source=(JAVA/'org/telegram/ui/Stars/StarGiftUniqueActionLayout.java').read_text()
  render=method(source,'    private void drawGiftBackground(').replace('app.exteraless.appearance.AppearanceConfig','AppearanceConfig').replace('app.freezgram.GiftGlassStyle','GiftGlassStyle')
  run_java('''
class PrivacyCheck {
 boolean glassBackground,colored;boolean hasColoredBackdrop(){return colored;}
 GiftGlassStyle.SoftCard softGiftBackground=new GiftGlassStyle.SoftCard();Object resourcesProvider;
 Paint backgroundPaint=new Paint(),glassHighlightPaint=new Paint(),glassEdgePaint=new Paint();
 RectF backgroundRect=new RectF(),glassEdgeRect=new RectF();float glassHighlightHeight=-1;
 static float dp(float n){return n;}
 RENDER
 public static void main(String[] args){
  var p=new PrivacyCheck();
  for(boolean colored:new boolean[]{false,true})for(boolean glass:new boolean[]{false,true}){
   p.colored=colored;p.glassBackground=glass;var c=new Canvas();p.drawGiftBackground(c);
   int fills=(!glass||colored?1:0)+(glass&&colored?1:0);
   if(c.fills!=fills||c.edges!=(glass&&colored?1:0))throw new AssertionError("Gift base or glass finish missing");
   if(colored&&c.first!=p.backgroundPaint)throw new AssertionError("Opaque gift gradient must draw first");
  }
  p.colored=p.glassBackground=true;var c=new Canvas();p.drawGiftBackground(c);
  Object shader=p.glassHighlightPaint.shader;p.drawGiftBackground(c);
  if(p.glassHighlightPaint.shader!=shader)throw new AssertionError("Highlight shader not reused");
  GiftGlassStyle.selected=2;c=new Canvas();p.drawGiftBackground(c);
  if(c.fills!=1||c.first!=p.backgroundPaint||c.soft!=1)throw new AssertionError("Soft NFT lost native gradient or finish");
 }
}
class Paint{Object shader;enum Style{FILL,STROKE}void setShader(Object o){shader=o;}void setStyle(Style s){}void setStrokeWidth(float f){}void setColor(int c){}void setAlpha(int a){}}
class GiftGlassStyle{static int ORDINARY=0,SOFT=2,selected=1;static boolean customOrdinary(){return false;}static void drawOrdinary(Canvas c,RectF r,float f,Object p,Object d){}static int style(){return selected;}static class SoftCard{void draw(Canvas c,RectF r,float rad,Object p,float a,boolean o){c.soft++;}}}
class AppearanceConfig{static int giftGlassIntensity(){return 70;}}
class Shader{enum TileMode{CLAMP}}
class LinearGradient{LinearGradient(float a,float b,float c,float d,int[] colors,float[] positions,Shader.TileMode t){}}
class RectF{float height(){return 200;}void set(RectF r){}void inset(float x,float y){}}
class Canvas{int fills,edges,soft;Paint first;void drawPaint(Paint p){if(first==null)first=p;fills++;}void drawRoundRect(RectF r,float x,float y,Paint p){edges++;}}
'''.replace('RENDER',render))

 def test_gift_cards_require_opt_in_supported_platform_blur_and_no_power_saver(self):
  policy=method((JAVA/'app/freezgram/SmallElementsGlass.java').read_text(),'    public static boolean areGiftCardsEnabled()')
  run_java('''
class PrivacyCheck{
 POLICY
 public static void main(String[] args){
  for(int sdk:new int[]{24,32,33,36})for(boolean opt:new boolean[]{false,true})for(int style:new int[]{0,1,2})for(boolean blur:new boolean[]{false,true})for(boolean saver:new boolean[]{false,true}){
   Build.VERSION.SDK_INT=sdk;AppearanceConfig.opt=opt;GiftGlassStyle.selected=style;SharedConfig.blur=blur;LiteMode.saver=saver;
   if(areGiftCardsEnabled()!=(opt&&style==1&&sdk>=33&&blur&&!saver))throw new AssertionError("Gift eligibility changed");
  }
 }
}
class Build{static class VERSION{static int SDK_INT;}}
class GiftGlassStyle{static int GLASS=1,selected;static int style(){return AppearanceConfig.opt?selected:0;}}
class AppearanceConfig{static boolean opt;static boolean giftCardsGlass(){return opt;}}
class SharedConfig{static boolean blur;static boolean chatBlurEnabled(){return blur;}}
class LiteMode{static boolean saver;static boolean isGlassPowerSaverApplied(){return saver;}}
'''.replace('POLICY',policy))

 def test_side_counter_glass_is_opt_in_and_has_a_separate_drawable(self):
  source=(JAVA/'org/telegram/ui/Components/CounterView.java').read_text()
  setup=method(source,'    public void setGlassBackground(')
  setup=setup.replace('org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory','Factory').replace('org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProvider','Object').replace('org.telegram.messenger.AndroidUtilities','AndroidUtilities').replace('app.freezgram.SmallElementsGlass','SmallElementsGlass').replace('app.freezgram.LiquidGlassStyle','LiquidGlassStyle')
  run_java('''
class PrivacyCheck{
 CounterDrawable counterDrawable=new CounterDrawable();
 SETUP
 public static void main(String[] args){
  var p=new PrivacyCheck();var q=new PrivacyCheck();var f=new Factory();p.setGlassBackground(f,null);q.setGlassBackground(f,null);
  var c=new Canvas();var r=new RectF();
  for(boolean enabled:new boolean[]{false,true})for(boolean hardware:new boolean[]{false,true}){
   SmallElementsGlass.enabled=enabled;c.hardware=hardware;int before=f.draws;
   boolean result=p.counterDrawable.backgroundDrawer.draw(c,r,12);
   if(result!=(enabled&&hardware)||f.draws-before!=(result?1:0))throw new AssertionError("Counter fallback missing");
  }
  if(f.created!=2)throw new AssertionError("Counter drawings share mutable material");
 }
}
class CounterDrawable{interface BackgroundDrawer{boolean draw(Canvas c,RectF bounds,float radius);}BackgroundDrawer backgroundDrawer;}
class Canvas{boolean hardware;boolean isHardwareAccelerated(){return hardware;}}
class Rect{}class RectF{void set(RectF r){}void round(Rect r){}}
class AndroidUtilities{static RectF rectTmp=new RectF();static Rect rectTmp2=new Rect();}
class Factory{int draws,created;Drawable create(Object v,Object p){created++;return new Drawable(this);}}
class Drawable{Factory owner;Drawable(Factory f){owner=f;}void setBounds(Rect r){}void setRadius(float r){}void updateColors(){}void draw(Canvas c){owner.draws++;}}
class SmallElementsGlass{static boolean enabled;static boolean isEnabled(){return enabled;}}
class LiquidGlassStyle{static void configure(Drawable d){}}
'''.replace('SETUP',setup))

 def test_side_controls_can_restore_glass_after_opening_in_power_saver(self):
  source=(JAVA/'org/telegram/ui/ChatActivity.java').read_text()
  start=source.index('        BlurredBackgroundDrawableViewFactory sideButtonsFactory =')
  end=source.index('        sideControlsButtonsLayout =',start)
  setup=source[start:end].replace('org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProvider','Colors')
  for prefix in ['app.exteraless.appearance.','app.freezgram.']:setup=setup.replace(prefix,'')
  run_java('''
class PrivacyCheck{
 BlurredBackgroundDrawableViewFactory glassBackgroundDrawableFactory=new BlurredBackgroundDrawableViewFactory(false),iosHeaderGlassFactory=new BlurredBackgroundDrawableViewFactory(false),picked;
 Object iosHeaderGlassSource=new Object(),resourceProvider;Colors blurredBackgroundColorProvider=new Colors(()->false),colors;
 void init(){SETUP picked=sideButtonsFactory;colors=sideButtonsColors;}
 public static void main(String[] args){
  var p=new PrivacyCheck();SmallElementsGlass.saver=true;p.init();
  if(!p.picked.glass||p.colors.active.getAsBoolean())throw new AssertionError("No restorable side source or saver ignored");
  SmallElementsGlass.saver=false;if(!p.colors.active.getAsBoolean())throw new AssertionError("Side glass cannot restore without recreating chat");
  AppearanceConfig.opt=false;p.init();if(p.picked.glass)throw new AssertionError("Opt-out ignored");
 }
}
class BlurredBackgroundDrawableViewFactory{boolean glass;BlurredBackgroundDrawableViewFactory(boolean g){glass=g;}BlurredBackgroundDrawableViewFactory withSource(Object s){return new BlurredBackgroundDrawableViewFactory(true);}void setDrawableConfigurator(java.util.function.Consumer<Object> c){}}
class Colors{java.util.function.BooleanSupplier active;Colors(java.util.function.BooleanSupplier e){active=e;}}
class AppearanceConfig{static boolean opt=true;static boolean smallElementsGlass(){return opt;}}
class Build{static class VERSION{static int SDK_INT=36;}}
class SharedConfig{static boolean chatBlurEnabled(){return true;}}
class SmallElementsGlass{static boolean saver;static boolean isEnabled(){return AppearanceConfig.opt&&!saver;}}
class LiquidGlassStyle{static void configure(Object d){}static Colors surfaceColors(Object r,java.util.function.BooleanSupplier e){return new Colors(e);}}
'''.replace('SETUP',setup))
