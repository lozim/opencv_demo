函数放在了cutpic的dabai的包里面，只需要调用scan函数，输入一个图片的base64 String，return
剪裁好的图片的 jpg格式的base64String   

这可能是我的第一个java程序吧，一开始的c++写的调opencv，但是在linux上部署好麻烦，
用了几天研究来写的，一个个困难解决下来感觉还是学到了很多东西，mat2base64， java与c++的
sort函数传的对比函数不同， c++要的一个bool类型的随便自己写的函数， java要的重写class的sort
方法的一个返回int的函数，图片的旋转还是镜像的问题，打包jar包的时候函数要放在一个package里面，
怎样封装jar包使得可以在别人的电脑上自动加载dll文件 loadlibrary ，关键字“同步” 

函数描述  
（1） private synchronized static void loadLib 是一个自动找dll的函数
（2） base2mat和mat2base的思路一样，都是用了byte数组作为中间交换介质
因为用了opencv的数据类型mat，用了opencv的函数imencode和imdecode，话说c++版本的解码表和编码表真的难看懂
（3） 预处理 getcanny函数能输出很漂亮的 canny二值结果
（4） 内部类LINE更像一个数据结构，只要有一个构造函数，下面重写了他自身的compare函数给collection.sort来调用
（5） 主函数scan其实是两个函数拼凑的，第一个是裁切矩形cut_maxcontour，第二个是找外围直线四个角点仿射变换hough_affine
cut_maxcontour 对canny图片找最大contourarea的contour，求该contour的minarearect，对这四个点做warpAffine
hough_affine 对canny图片做霍夫直线检测，求最外围直线的四个交点，用这四个顶点做仿射变换，因为这四个点不是矩形的四个点
，所以要自己去找映射结果的点，这就要考虑这四个点的相对关系，这就引出了镜像的问题（我自己总结就是只要四点的顺逆时针顺序不变，就不是镜像，反之）。

