# KuangBianXiaoHaiLuo / 狂扁小海螺
The Construction Site of Mini Game Plugin KuangBianXiaoHaiLuo / 狂扁小海螺小游戏插件的施工现场

## 策划

* 玩家输入 `/kbxhl start` 命令开始小游戏
  * 玩家的背包将会清空，手中固定手持一把石斧
  * 玩家脚下 3x3 的区域将会不定期刷新显示名称为“海螺”的海龟
  * 海龟的显示时间极短，玩家必须在海龟消失前击中海龟，否则不能得分
  * 玩家对海龟使用左键海龟消失，同时玩家获得一个海龟壳，一个海龟壳计一分
  * 共获得 651 个海龟壳结束游戏，统计整个游戏的耗费时间
* 玩家输入 `/kbxhl stop` 强制结束游戏
  * 游戏结束时背包归位
  * 游戏进行中背包内物品不得移动
* 玩家输入 `/kbxhl top` 显示排行榜
  * 所有正常结束游戏的玩家都将会将成绩自动上传排行榜
  * 排行榜按耗费时间从小到大正序排列


## 命令

* `/kbxhl`
  * `/kbxhl start`
  * `/kbxhl stop`
  * `/kbxhl top`

## 权限

* `kbxhl.command`：所有相关权限
  * `kbxhl.command.start`：开始游戏的权限
  * `kbxhl.command.stop`：结束游戏的权限
  * `kbxhl.command.top`：显示排行榜的权限
