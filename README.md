# MiDisplayRate

适用于小米Mix Fold 2（MIUI FOLD 13.1.21）的“伪LTPO”APP

FOLD2外屏的可变刷新率策略是：通常情况下120Hz，打开某些App会切换到60Hz，这导致了某些场景，比如静态地阅读文章、浏览图片时也是120Hz，令人不爽。于是开发这一款App，希望在用户操作的时候120Hz，静止时60Hz。

## 实现原理
1. 在屏幕上放置一个Overlay Window，这样可以监听到用户的手势（至少是手势的开始），这时候切换到120Hz，然后启动计时器，1500毫秒之后切换回60Hz。
1. 切换刷新率的方式是通过打开speed_mode打开120Hz，更新Overlay Window LayoutParams的rate相关属性降低到60Hz。因为反编译了MIUI FOLD 13.1.21中的com.miui.powerkeeper，从中看出逻辑，锁60Hz是通过更改miui_refresh_rate的值实现的，但是这个值在Settings.Secure中，app没有权限修改，所以某些锁定了60的app（如抖音）无法提高到120Hz，但是又发现Settings.System中有一个speed_mode开关，开了之后可以全局120Hz

## 注意的问题
1. 外屏素质限制，切换刷新率时会闪一下，在晚上深色模式，屏幕灰色元素较多时会看得到，平时看不到（没有LTPO的屏幕就是这样，可能也是小米不做动态刷新的原因吧）
1. 这只是个自我娱乐项目，不负责稳定更新，编译此App的开发者必须确保能理解所有源码，并对可能产生的问题有能力解决
1. 通常请求切换刷新率的接口是修改WindowManager.LayoutParams.preferredDisplayModeId，但是小米似乎在framework做了一些改动，会导致结果不符合预期，比如从120切到60的时候，虽然会正确引起SurfaceFlinger的更新，但是maxRate总是120，就很奇怪，最后是反射修改了WindowManager.LayoutParams.preferredMaxDisplayRefreshRate。这个属性是@hide属性，所以很可能说不定哪天小米就改了
1. 项目非常早期，还是demo阶段，什么申请权限、用户界面什么的都没搞，无障碍和修改系统设置权限先去系统设置里手动开启

## 后续计划
1. 后续可能修改切换刷新率的方式，因为不确定speed_mode是不是仅针对屏幕刷新率，目前是没有在com.miui.powerkeeper反编译的文件中看到除刷新率之外的使用，但不保证不影响其他系统特性。
之后的切换刷新率的方式是通过设置miui_refresh_rate值的方式，com.miui.powerkeeper中也是这么做的，应该说这么做比较接近官方逻辑，但由于这个值在Secure中，所以到时候还需要依赖Shizuku服务。
