# KuangBianXiaoHaiLuo / 狂扁小海螺

![built-with-love](https://img.shields.io/badge/built%20with-%E2%9D%A4-red.svg)
![github-last-commit](https://img.shields.io/github/last-commit/ustc-zzzz/KuangBianXiaoHaiLuo.svg?color=yellow)
![github-license](https://img.shields.io/github/license/ustc-zzzz/KuangBianXiaoHaiLuo.svg)
![github-release](https://img.shields.io/github/release/ustc-zzzz/KuangBianXiaoHaiLuo.svg)
![officially-authorized-by-izzel-aliz](https://img.shields.io/badge/officially%20authorized%20by-IzzelAliz-purple.svg)

A whac-a-mole-like mini game plugin / 像打地鼠一样的小游戏插件

## 插件下载

<https://github.com/ustc-zzzz/KuangBianXiaoHaiLuo/releases/latest>

插件同时支持 Bukkit 及 Sponge 平台

## 插件策划

* 玩家输入 `/kbxhl start` 命令开始小游戏
  * 玩家的背包将会清空，手中固定手持一把石斧
  * 玩家四周各 3x3 共计 36 格区域将不定期刷新显示名称为“海螺”的潜影贝
  * 潜影贝的显示时间极短，玩家必须在潜影贝消失前击中潜影贝，否则玩家不能得分
  * 玩家对潜影贝使用左键潜影贝消失，同时玩家获得对应的潜影壳，一个潜影壳代表一分
  * 玩家获得不少于 651 个潜影壳，亦即 651 分时结束游戏，此时统计整个游戏的耗费时间

* 玩家输入 `/kbxhl stop` 强制结束游戏
  * 游戏结束时背包归位
  * 游戏进行中背包内物品不得移动

* 玩家输入 `/kbxhl top` 显示排行榜
  * 所有正常结束游戏的玩家都将会将成绩自动上传排行榜
  * 排行榜按耗费时间从小到大正序排列，最多显示十名玩家的记录

## 插件数值

* 普通海螺（N）：紫色，可获得 1 个潜影壳，属于出现概率最高的海螺
* 稀有海螺（R）：粉色，可获得 5 个潜影壳，出现概率是普通海螺的二分之一
* 超级稀有海螺（SR）：橙色，可获得 25 个潜影壳，出现概率是稀有海螺的三分之一
* 特级稀有海螺（SSR）：黄色，可获得 125 个潜影壳，出现概率是超级稀有海螺的二分之一

## 插件命令

* `/kbxhl`：总命令
  * `/kbxhl start`：开始游戏
  * `/kbxhl stop`：强制结束游戏
  * `/kbxhl top`：显示玩家排行榜

## 插件权限

* `kbxhl.command.start`：开始游戏的权限
* `kbxhl.command.stop`：结强制束游戏的权限
* `kbxhl.command.top`：显示玩家排行榜的权限

## 源码编译

Linux/macOS:

* `git clone https://github.com/ustc-zzzz/KuangBianXiaoHaiLuo.git`
* `cd KuangBianXiaoHaiLuo`
* `./gradlew`

Windows:

* `git clone https://github.com/ustc-zzzz/KuangBianXiaoHaiLuo.git`
* `cd KuangBianXiaoHaiLuo`
* `gradlew.bat`

请事先保证计算机已安装 Git 及 JDK 8
