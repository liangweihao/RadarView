# RadarView
仿照战力分析图的自定义View
后期拓展内部数值分析
战力图点击查看详情
其颜色 文字大小 间距 半径 分段数量都可以调整


内部的数据一部分是为了测试
//           测试数据    随机点
            distance =(float) Math.random();
            distance = (float) (distance < 0.2 ? 0.2 : distance) * mRadius;

这里注释了即可
innerPointDistance[i]; 是内部点的数组 



![演示](https://github.com/liangweihao/RadarView/blob/master/QQ20171021-141322-HD.gif?raw=true)；
