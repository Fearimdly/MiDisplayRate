# MiDisplayRate

适用于小米Mix Fold 2（MIUI FOLD 13.1.21）的“伪LTPO”APP

FOLD2外屏的可变刷新率策略是：通常情况下120Hz，打开某些App会切换到60Hz，这导致了某些场景，比如静态地阅读文章、浏览图片时也是120Hz，令人不爽。于是开发这一款App，希望在用户操作的时候120Hz，静止时60Hz。

## 实现原理
1. 在屏幕上放置一个Overlay Window，这样可以监听到用户的手势（至少是手势的开始），这时候切换到120Hz，然后启动计时器，1500毫秒之后切换回60Hz。
1. 切换刷新率的方式是更新Overlay Window LayoutParams的rate相关属性

## 注意的问题
1. 这只是个自我娱乐项目，不负责稳定更新，编译此App的开发者必须确保能理解所有源码，并对可能产生的问题有能力解决
1. 通常请求切换刷新率的接口是修改WindowManager.LayoutParams.preferredDisplayModeId，但是小米似乎在framework做了一些改动，会导致结果不符合预期，比如从120切到60的时候，虽然会正确引起SurfaceFlinger的更新，但是maxRate总是120，就很奇怪，最后是反射修改了WindowManager.LayoutParams.preferredMaxDisplayRefreshRate。这个属性是@hide属性，所以很可能说不定哪天小米就改了
1. 项目非常早期，还是demo阶段，什么申请权限、用户界面什么的都没搞，有需要的权限先去系统设置里手动开启
1. 某些被小米锁了60Hz的App，比如抖音等无法切换到120Hz
1. 前提是系统设置显示刷新率是120Hz

## 后续计划
1. Service换成AccessibilityService，因为Accessibility Window可以在尽可能高的位置，比如可以覆盖导航条，目前触摸导航条、侧边返回手势、通知栏都接收不到手势
1. 换成AccessibilityService可以监听到一些更准确的界面变化，可以让切换更准确一些
