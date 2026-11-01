"""Composer policy and surface geometry without an Android build."""
import unittest
import shutil
from pathlib import Path
from test_personal_privacy import method
from glass_test_support import run_java

JAVA = Path(__file__).resolve().parents[1] / "TMessagesProj/src/main/java"

@unittest.skipUnless(shutil.which("java"), "Java source launcher required")
class ComposerGlassTests(unittest.TestCase):
    def test_ios_text_and_emoji_spacing_and_restoration(self):
        source=(JAVA / 'org/telegram/ui/Components/ChatActivityEnterView.java').read_text()
        spacing=method(source, '    private void applyIosComposerSpacing()').replace('app.freezgram.ChatComposerGlass','ChatComposerGlass')
        policy=(JAVA / 'app/freezgram/ChatComposerGlass.java').read_text()
        helpers='\n'.join(method(policy, sig) for sig in (
            '    public static int textLeftInsetDp()', '    public static int emojiRightInsetDp()',
            '    public static int textRightInsetDp('))
        run_java('''
class PrivacyCheck {
 boolean iosComposerLayout; View emojiButton=new View(),messageEditText=new View();
 static int dp(int value){return value;}
 SPACING
 public static void main(String[] args){
  var view=new PrivacyCheck();
  for(int repeat=0;repeat<3;repeat++){
   int text=repeat*16,emoji=text;
   AppearanceConfig.text=text;AppearanceConfig.emoji=emoji;view.iosComposerLayout=true;
   view.applyIosComposerSpacing();
   if(view.messageEditText.params.leftMargin!=57+text||view.emojiButton.params.rightMargin!=emoji)
    throw new AssertionError("Text/emoji must stay inside the shifted field");
   if(ChatComposerGlass.textRightInsetDp(44)!=52+emoji)throw new AssertionError("Emoji reservation");
   view.iosComposerLayout=false;view.messageEditText.params.leftMargin=50;
   view.applyIosComposerSpacing();
   if(view.emojiButton.params.rightMargin!=0||view.messageEditText.params.leftMargin!=50)
    throw new AssertionError("Special states must keep legacy layout");
  }
 }
}
class View{FrameLayout.LayoutParams params=new FrameLayout.LayoutParams();Object getLayoutParams(){return params;}}
class MarginLayoutParams{int leftMargin,rightMargin;}
class FrameLayout{static class LayoutParams extends MarginLayoutParams{}}
class ChatComposerGlass{HELPERS}
class AppearanceConfig{static int composerContentInset(){return text;}static int text,emoji;static int composerTextInset(){return text;}static int composerEmojiInset(){return emoji;}}
'''.replace('SPACING',spacing).replace('HELPERS',helpers))

    def test_bottom_gap_moves_surface_and_controls_and_reserves_space(self):
        text=(JAVA / 'org/telegram/ui/Components/chat/ChatInputViewsContainer.java').read_text()
        methods='\n'.join(method(text,sig) for sig in (
            '    public int getInputLiftPx()', '    public float getInputBubbleOccupiedHeight()',
            '    private void checkViewsPositions()', '    private void checkBlurredHeight(boolean force)',
            '    public float getInputBubbleHeight()', '    public float getInputBubbleTop()',
            '    public float getInputBubbleBottom()'))
        methods=methods.replace('app.exteraless.appearance.AppearanceConfig','AppearanceConfig')
        run_java('''
class PrivacyCheck {
 RectF tmpRectF=new RectF();Path underKeyboardPath=new Path();
 static int INPUT_KEYBOARD_RADIUS=29;void invalidate(){}int getMeasuredWidth(){return 400;}
 static int INPUT_BUBBLE_BOTTOM=9;int inputBubbleHeightRound=44,currentBlurredHeight;float inputBubbleHeight=44,maxBottomInset,imeBottomInset;
 View inputIslandBubbleContainer=new View(),inAppKeyboardBubbleContainer=new View();
 int getMeasuredHeight(){return 1000;}static int dp(int value){return value;}
 METHODS
 public static void main(String[] args){
  var p=new PrivacyCheck();
  for(int style:new int[]{0,1,2})for(int gap:new int[]{0,16,64})for(int inset:new int[]{20,300}){
   AppearanceConfig.style=style;AppearanceConfig.gap=gap;p.maxBottomInset=inset;p.imeBottomInset=inset;
   int expected=style==1?gap:0;p.checkBlurredHeight(true);
   if(p.inputIslandBubbleContainer.translation!=-inset-9-expected)throw new AssertionError("Controls not lifted");
   if(p.getInputBubbleBottom()!=1000-inset-9-expected||p.getInputBubbleTop()!=1000-inset-9-expected-44)
    throw new AssertionError("Visual and hit-test bounds disagree");
   if(p.currentBlurredHeight!=44+9+inset+expected||p.getInputBubbleOccupiedHeight()!=44+expected||p.getInputBubbleHeight()!=44)
    throw new AssertionError("Gap missing from capture or content padding");
  }
 }
}
class RectF{void set(float l,float t,float r,float b){}}
class Path{enum Direction{CW}void rewind(){}void close(){}void addRoundRect(RectF r,float[] radii,Direction d){}}
class View{float translation;void setTranslationY(float value){translation=value;}int getMeasuredHeight(){return 250;}}
class AppearanceConfig{static int style,gap;static int chatComposerStyle(){return style;}static int composerBottomLift(){return gap;}}
'''.replace('METHODS',methods))

    def test_side_insets_are_symmetric_and_keep_minimum_input_width(self):
        text=(JAVA / 'org/telegram/ui/Components/chat/ChatInputViewsContainer.java').read_text()
        apply=(method(text, '    public int getInputSideInsetForWidth(int width)')+'\n'+method(text, '    private void applyInputSideInsets(int width)')).replace('app.exteraless.appearance.AppearanceConfig','AppearanceConfig')
        run_java('''
class PrivacyCheck {
 int inputSideInset;View inputIslandBubbleContainer=new View();static int dp(int n){return n*2;}
 APPLY
 public static void main(String[] args){
  var p=new PrivacyCheck();
  for(int style:new int[]{0,1,2})for(int side:new int[]{0,16,48})for(int width:new int[]{440,480,640,824}){
   AppearanceConfig.style=style;AppearanceConfig.side=side;p.applyInputSideInsets(width);
   int expected=style==1?Math.min(side*2,Math.max(0,(width-480)/2)):0;
   if(p.inputSideInset!=expected||p.inputIslandBubbleContainer.params.leftMargin!=expected||p.inputIslandBubbleContainer.params.rightMargin!=expected)
    throw new AssertionError("Whole panel must have equal left/right margins");
  }
 }
}
class LayoutParams{int leftMargin,rightMargin;}
class View{LayoutParams params=new LayoutParams();Object getLayoutParams(){return params;}void setLayoutParams(LayoutParams p){params=p;}}
class AppearanceConfig{static int style,side;static int chatComposerStyle(){return style;}static int composerSideInset(){return side;}}
'''.replace('APPLY',apply))

    def test_send_transition_keeps_permanent_panel_gap(self):
        source=(JAVA / 'org/telegram/ui/ChatActivity.java').read_text()
        collapse=method(source,'    public float getInputHeightChangeOnSend()')
        run_java('''
class PrivacyCheck {
 float inputIslandHeightCurrent;Container chatInputViewsContainer=new Container();
 static int dp(int value){return value*2;}
 COLLAPSE
 public static void main(String[] args){
  var p=new PrivacyCheck();
  for(int gap:new int[]{0,16,64})for(int height:new int[]{44,88,180}){
   p.chatInputViewsContainer.gap=dp(gap);p.inputIslandHeightCurrent=dp(height+gap);
   float delta=p.getInputHeightChangeOnSend();
   if(delta!=dp(height-44))throw new AssertionError("Permanent gap leaked into transition endpoint");
   float oldListPadding=dp(30+height+gap);
   if(oldListPadding-delta!=dp(30+44+gap))throw new AssertionError("Final padding must preserve the gap");
  }
  p.chatInputViewsContainer=null;p.inputIslandHeightCurrent=dp(44);
  if(p.getInputHeightChangeOnSend()!=0)throw new AssertionError("Legacy baseline changed");
 }
}
class Container{int gap;int getInputLiftPx(){return gap;}}
'''.replace('COLLAPSE',collapse))

    def test_ios_record_button_stays_transparent_when_group_controls_change_layout(self):
        source=(JAVA / 'app/freezgram/ChatComposerGlass.java').read_text()
        methods=method(source,'    public static boolean shouldDrawRecordButtonBackground(')+'\n'+method(source,'    public static boolean useIosLayout(')
        run_java('''
class PrivacyCheck {
 METHODS
 public static void main(String[] args){
  for(int style:new int[]{0,1,2})for(boolean forbidden:new boolean[]{false,true})for(boolean menu:new boolean[]{false,true}){
   AppearanceConfig.style=style;
   if(shouldDrawRecordButtonBackground(forbidden,menu)!=(style!=1&&!forbidden&&!menu))throw new AssertionError("Wrong background policy");
   if(style==1){
    if(useIosLayout(true,320)||useIosLayout(false,200))throw new AssertionError("Fallback layout changed");
    if(shouldDrawRecordButtonBackground(false,false))throw new AssertionError("Group/narrow fallback must not restore blue recording surface");
   }
  }
 }
}
class AppearanceConfig{static int style;static int chatComposerStyle(){return style;}static boolean composerLiquidGlass(){return style!=0;}}
'''.replace('METHODS',methods))

    def test_policy_and_surface_bounds(self):
        source = (JAVA / "app/freezgram/ChatComposerGlass.java").read_text()
        methods = "\n".join(method(source, sig) for sig in (
            "    public static boolean isRequested()", "    public static boolean isEnabled()",
            "    public static boolean useIosLayout(", "    public static void surfaceBounds("))
        run_java('''
class PrivacyCheck {
 METHODS
 public static void main(String[] args) {
  for(int style:new int[]{0,1,2}) for(int sdk:new int[]{23,31,33,36})
  for(boolean blur:new boolean[]{false,true}) for(boolean power:new boolean[]{false,true}) {
   AppearanceConfig.style=style; Build.VERSION.SDK_INT=sdk;SharedConfig.blur=blur;LiteMode.power=power;
   if(isRequested()!=(style!=0))throw new AssertionError("Default changed");
   if(isEnabled()!=(style!=0&&sdk>=33&&blur&&!power))throw new AssertionError("Unsafe fallback");
   if(useIosLayout(false,320)!=(style==1))throw new AssertionError("Wrong layout");
   if(useIosLayout(true,320)||useIosLayout(false,200))throw new AssertionError("Complex/narrow layout must remain whole");
  }
  for(int width:new int[]{240,320,412,800})for(int height:new int[]{44,88,180}) {
   Rect all=new Rect(0,100,width,100+height+14),field=new Rect(),left=new Rect(),right=new Rect();
   surfaceBounds(all,field,left,right,true);
   if(left.left!=0||left.right!=58||right.left!=width-58||right.right!=width)throw new AssertionError("Button bounds");
   if(left.height()!=58||right.height()!=58||left.bottom!=all.bottom)throw new AssertionError("Circles must stay bottom aligned");
   if(field.left!=50||field.right!=width-50||field.height()!=all.height())throw new AssertionError("Multiline field bounds");
   if(left.right-7>=field.left+7||field.right-7>=right.left+7)throw new AssertionError("Surfaces overlap");
   surfaceBounds(all,field,left,right,false);
   if(field.left!=all.left||field.right!=all.right||field.top!=all.top||field.bottom!=all.bottom||left.height()!=0||right.height()!=0)throw new AssertionError("Whole layout not restored");
  }
 }
 static int dp(int n){return n;}
}
class AppearanceConfig{static int style;static int chatComposerStyle(){return style;}static boolean composerLiquidGlass(){return style!=0;}}
class Build{static class VERSION{static int SDK_INT;}}
class SharedConfig{static boolean blur;static boolean chatBlurEnabled(){return blur;}}
class LiteMode{static boolean power;static boolean isPowerSaverApplied(){return power;}}
class Rect {
 int left,top,right,bottom;
 Rect(){} Rect(int l,int t,int r,int b){set(l,t,r,b);}
 void set(int l,int t,int r,int b){left=l;top=t;right=r;bottom=b;}
 void set(Rect r){set(r.left,r.top,r.right,r.bottom);}
 void setEmpty(){set(0,0,0,0);} int height(){return bottom-top;}
}
'''.replace("METHODS", methods).replace("AndroidUtilities.dp", "dp"))

    def test_glass_fallback_and_animation_alpha(self):
        source = (JAVA / "app/freezgram/ChatComposerGlass.java").read_text()
        methods = "\n".join(method(source, sig) for sig in (
            "    public static boolean isRequested()", "    public static boolean isEnabled()",
            "    public static BlurredBackgroundProvider colors(", "    public static void refresh("))
        container = (JAVA / "org/telegram/ui/Components/chat/ChatInputViewsContainer.java").read_text()
        alpha = method(container, "    public void setInputBubbleAlpha(")
        run_java('''
class PrivacyCheck {
 METHODS
 BlurredBackgroundDrawable blurredBackgroundDrawable=new BlurredBackgroundDrawable();
 BlurredBackgroundDrawable composerLeftDrawable=new BlurredBackgroundDrawable(),composerRightDrawable=new BlurredBackgroundDrawable();
 ALPHA
 public static void main(String[] args) {
  for(int style:new int[]{0,1,2}) for(int sdk:new int[]{23,31,32,33,36})
  for(boolean blur:new boolean[]{false,true}) for(boolean power:new boolean[]{false,true}) {
   AppearanceConfig.style=style;Build.VERSION.SDK_INT=sdk;SharedConfig.blur=blur;LiteMode.power=power;
   boolean enabled=style!=0&&sdk>=33&&blur&&!power;
   BlurredBackgroundDrawableRenderNode drawable=new BlurredBackgroundDrawableRenderNode();
   drawable.enabled=sdk>=33;refresh(drawable);
   if(drawable.enabled!=enabled||drawable.thickness!=12||drawable.intensity!=.55f)throw new AssertionError("Lens fallback");
   for(boolean dark:new boolean[]{false,true}){
    int alpha=colors(null).background.getColor(null,dark)>>>24;
    if(enabled?(alpha<15||alpha>30):alpha<200)throw new AssertionError("Surface contrast");
   }
  }
  refresh(null);refresh(new BlurredBackgroundDrawable());
  var container=new PrivacyCheck();
  for(int a:new int[]{0,64,128,255}){
   container.setInputBubbleAlpha(a);
   if(container.blurredBackgroundDrawable.alpha!=a||container.composerLeftDrawable.alpha!=a||container.composerRightDrawable.alpha!=a)throw new AssertionError("Detached circles during animation");
  }
 }
}
class AppearanceConfig{static int style;static int chatComposerStyle(){return style;}static boolean composerLiquidGlass(){return style!=0;}}
class Build{static class VERSION{static int SDK_INT;}}
class SharedConfig{static boolean blur;static boolean chatBlurEnabled(){return blur;}}
class LiteMode{static boolean power;static boolean isPowerSaverApplied(){return power;}}
class AndroidUtilities{static int dp(int n){return n;}static float dpf2(float n){return n;}}
class Theme {
 static class ResourcesProvider{} static int key_chat_messagePanelBackground,key_windowBackgroundWhite;
 static int getColor(int key,ResourcesProvider r){return 0xffeeeeee;}
 static int multAlpha(int color,float alpha){return (color&0xffffff)|((int)(255*alpha)<<24);}
}
class BlurredBackgroundDrawable {
 int alpha,thickness;float intensity;
 void setAlpha(int a){alpha=a;}void setThickness(int t){thickness=t;}void setIntensity(float i){intensity=i;}
}
class BlurredBackgroundDrawableRenderNode extends BlurredBackgroundDrawable {
 boolean enabled;void setLiquidGlassEffectAllowed(boolean e){enabled=e;}
}
class BlurredBackgroundProvider {
 interface ColorProvider{int getColor(Theme.ResourcesProvider r,boolean dark);}
 ColorProvider background;
}
class BlurredBackgroundProviderBuilder extends BlurredBackgroundProvider {
 BlurredBackgroundProviderBuilder(Theme.ResourcesProvider r){}
 BlurredBackgroundProviderBuilder setBackgroundColor(ColorProvider p){background=p;return this;}
 BlurredBackgroundProviderBuilder setStrokeColorTop(int a,int b){return this;}
 BlurredBackgroundProviderBuilder setStrokeColorBottom(int a,int b){return this;}
 BlurredBackgroundProviderBuilder setStrokeWidth(float a,float b){return this;}
 BlurredBackgroundProviderBuilder setShadowColor(int a,int b){return this;}
 BlurredBackgroundProviderBuilder setShadowLayer(float a,float b,float c){return this;}
 BlurredBackgroundProvider build(){return this;}
}
'''.replace("METHODS",methods).replace("ALPHA",alpha))

    def test_ios_falls_back_for_special_controls_but_keeps_channel_notifications(self):
        source = (JAVA / "org/telegram/ui/Components/ChatActivityEnterView.java").read_text()
        methods = "\n".join(method(source,sig) for sig in (
            "    private static boolean composerControlVisible(", "    private boolean canUseIosComposerLayout("))
        policy = method((JAVA / "app/freezgram/ChatComposerGlass.java").read_text(), "    public static boolean useIosLayout(")
        run_java('''
class PrivacyCheck {
 static int VISIBLE=0;
 boolean isStories,isLiveComment,recordingAudioVideo,stickers;
 Object editingMessageObject;
 View recordedAudioPanel,recordPanel,topView,senderSelectView,botCommandsMenuButton,
 botWebViewButton,botButton,deleteRichDraftButton,richDraftPreview;
 View attachButton=new View(),emojiButton=new View();
 METHODS
 boolean isStickersExpanded(){return stickers;}
 public static void main(String[] args) {
  var enter=new PrivacyCheck();
  if(!enter.canUseIosComposerLayout(320)||enter.canUseIosComposerLayout(200))throw new AssertionError("Normal input geometry");
  for(int condition=0;condition<13;condition++){
   enter=new PrivacyCheck();
   switch(condition){
    case 0:enter.isStories=true;break;case 1:enter.isLiveComment=true;break;
    case 2:enter.recordingAudioVideo=true;break;case 3:enter.editingMessageObject=new Object();break;
    case 4:enter.recordedAudioPanel=new View();break;case 5:enter.recordPanel=new View();break;
    case 6:enter.topView=new View();break;case 7:enter.senderSelectView=new View();break;
    case 8:enter.botWebViewButton=new View();break;case 9:enter.botButton=new View();break;
    case 10:enter.deleteRichDraftButton=new View();break;case 11:enter.richDraftPreview=new View();break;
    case 12:enter.stickers=true;break;
   }
   if(enter.canUseIosComposerLayout(320))throw new AssertionError("Special controls overlap: "+condition);
  }
 }
}
class View{int getVisibility(){return 0;}Object getTag(){return null;}}
class AndroidUtilities{static float density=1;}
class ChatComposerGlass{POLICY}
class AppearanceConfig{static int chatComposerStyle(){return 1;}}
'''.replace("METHODS",methods).replace("POLICY",policy).replace("app.freezgram.ChatComposerGlass", "ChatComposerGlass"))

    def test_preliminary_measurement_uses_the_same_width_as_nested_composer(self):
        container=(JAVA/'org/telegram/ui/Components/chat/ChatInputViewsContainer.java').read_text()
        inset=method(container,'    public int getInputSideInsetForWidth(int width)').replace('app.exteraless.appearance.AppearanceConfig','AppearanceConfig')
        chat=(JAVA/'org/telegram/ui/ChatActivity.java').read_text()
        start=chat.index('            measureChildWithMargins(chatActivityEnterView, widthMeasureSpec,')
        call=chat[start:chat.index(';',start)+1]
        run_java('''
class PrivacyCheck{
 Object chatActivityEnterView=new Object();Panel chatInputViewsContainer=new Panel();int measuredWidth;
 void measureChildWithMargins(Object v,int width,int used,int height,int extra){measuredWidth=width-used-14;}
 void measure(int widthSize){int widthMeasureSpec=widthSize,heightMeasureSpec=1000;CALL}
 public static void main(String[] args){
  var p=new PrivacyCheck();
  for(int style:new int[]{0,1,2})for(int side:new int[]{0,16,48})for(int width:new int[]{220,240,360,412}){
   AppearanceConfig.style=style;AppearanceConfig.side=side;p.measure(width);
   int nested=width-2*p.chatInputViewsContainer.getInputSideInsetForWidth(width)-14;
   if(p.measuredWidth!=nested)throw new AssertionError("Draft has conflicting wrap widths");
   // A draft just above the nested one-line width must wrap in both passes.
   int draftWidth=nested+1;
   if((draftWidth>p.measuredWidth)!=(draftWidth>nested))throw new AssertionError("Line count oscillates");
  }
 }
}
class Panel{static int dp(int n){return n;}INSET}
class AppearanceConfig{static int style,side;static int chatComposerStyle(){return style;}static int composerSideInset(){return side;}}
'''.replace('CALL',call).replace('INSET',inset))

    def test_send_glass_replaces_accent_fill_and_keeps_fallback(self):
        text=(JAVA/'org/telegram/ui/Components/ChatActivityEnterView.java').read_text()
        def inner(signature):
            start=text.index(signature)
            return text[start:text.index('\n        }',start)+len('\n        }')]
        methods=inner('        private boolean useComposerGlass()')+'\n'+inner('        private boolean drawComposerGlassBackground(Canvas canvas)')
        start=text.index('            final boolean glassSend = drawComposerGlassBackground(canvas);')
        end=text.index('            final boolean inactive =',start)
        paint=text[start:end]
        run_java('''
class PrivacyCheck{
 boolean composerGlassButton,isNewDesignSendButton=true;Drawable blurredBackgroundDrawable;
 Rect backgroundRect=new Rect();Object backgroundPaint;static int RADIUS=22;static int dp(int n){return n;}
 METHODS
 void render(Canvas canvas){PAINT}
 public static void main(String[] args){
  for(int style:new int[]{0,1,2})for(boolean opt:new boolean[]{false,true})for(boolean supported:new boolean[]{false,true})
  for(boolean hardware:new boolean[]{false,true})for(boolean material:new boolean[]{false,true}){
   var p=new PrivacyCheck();var c=new Canvas();c.hardware=hardware;p.composerGlassButton=opt;
   AppearanceConfig.style=style;ChatComposerGlass.enabled=supported;p.blurredBackgroundDrawable=material?new Drawable():null;
   p.render(c);boolean glass=style==1&&opt&&supported&&hardware&&material;
   if(c.blue!=(glass?0:1)||(material?p.blurredBackgroundDrawable.draws:0)!=(glass?1:0))throw new AssertionError("Accent covers glass or fallback missing");
  }
 }
}
class Canvas{boolean hardware;int blue;boolean isHardwareAccelerated(){return hardware;}void drawRoundRect(Object r,int x,int y,Object paint){blue++;}}
class Rect{void round(Object r){}}
class Drawable{int draws;void setBounds(Object r){}void updateColors(){}void draw(Canvas c){draws++;}}
class AndroidUtilities{static Object rectTmp2=new Object();}
class AppearanceConfig{static int style;static int chatComposerStyle(){return style;}static boolean composerLiquidGlass(){return style!=0;}}
class ChatComposerGlass{static boolean enabled;static boolean isEnabled(){return enabled;}static void refresh(Drawable d){}}
'''.replace('METHODS',methods).replace('PAINT',paint).replace('app.exteraless.appearance.AppearanceConfig','AppearanceConfig').replace('app.freezgram.ChatComposerGlass','ChatComposerGlass'))
