import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class fitt {

    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }
    public static String mat2base(Mat matrix, String fileExtension) {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(fileExtension, matrix, mob);
        byte[] byteArray = mob.toArray();
        //再在这里把byte数组变成base64就好了
        final Base64.Encoder encoder = Base64.getEncoder();
        final String encodedText = encoder.encodeToString(byteArray);
        return  encodedText;
    }
    public static  Mat base2mat (String base_data ){
        final Base64.Decoder decoder = Base64.getDecoder();
        byte [] bytearray =  decoder.decode(base_data);
        Mat mat = Imgcodecs.imdecode(new MatOfByte(bytearray), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
        return mat;

    }
    //预处理
    static Mat getCanny (Mat gray){
        Mat res = new Mat();
        Mat thres =new Mat() ;
        double high_thres = Imgproc.threshold(gray, thres, 0, 255, Imgproc.THRESH_BINARY| Imgproc.THRESH_OTSU), low_thres = high_thres * 0.5;
        Imgproc.Canny(gray, res, low_thres, high_thres);
        return res;
//        Mat thres  =new Mat();
//        Mat res = new Mat();
//        Imgproc.threshold(gray,thres,0,255,Imgproc.THRESH_OTSU);
//        Mat dil_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(3,3));
//        Imgproc.dilate(thres,thres,dil_kernel);
//        Mat ero_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(5,5));
//        Imgproc.erode(thres,thres,ero_kernel);
//        Imgproc.dilate(thres,thres,dil_kernel);
//        Imgproc.erode(thres,thres,ero_kernel);
//        Imgproc.dilate(thres,thres,dil_kernel);
//        Imgproc.erode(thres,thres,ero_kernel);
//        Imgcodecs.imwrite("f:/33.jpg",thres);

//        //Imgproc.dilate(thres);
//        Imgproc.Canny(thres,res,100,255);
//        Imgcodecs.imwrite("f:/44.jpg",res);
//        return res;
    }
    //写一个class容易找到中心point
    static class LINE{
        Point _p1;
        Point _p2;
        Point _center;
        LINE( Point p1, Point p2) {
            _p1 = p1;
            _p2 = p2;
            _center = new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
        }
    }
    //写一个函数给sort来对比
    static class cmp_y implements Comparator<LINE>{
        @Override
        public  int compare(final LINE p1, final LINE p2){
            //return  (int)(p1._center.y < p2._center.y);
            if(p1._center.y < p2._center.y)
                return 1;
            else return -1;

        }
    }
    static class cmp_x implements Comparator<LINE>{
        @Override
        public int compare(final LINE p1, final LINE p2){
            //return p1._center.x < p2._center.x;
            if(p1._center.x < p2._center.x)
                return 1;
            else return -1;
        }
    }
    //算两条线的交点
    static Point computeIntersect(LINE l1, LINE l2) {
        int x1 = (int)l1._p1.x, x2 = (int)l1._p2.x, y1 = (int)l1._p1.y, y2 = (int)l1._p2.y;
        int x3 = (int)l2._p1.x, x4 = (int)l2._p2.x, y3 = (int)l2._p1.y, y4 = (int)l2._p2.y;
        float d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4) ;
        Point pt = new Point();
        pt.x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
        pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
        return pt;
    }
    //主函数
    static  String scan(String base64_data){
        //做一些缩放
        Mat img = base2mat(base64_data);
//        Mat img_proc=new Mat();
//        int w = (int)img.size().width;
//        int h = (int)img.size().height, min_w = 200;
//        double scale = Math.min(10.0, w * 1.0 / min_w);
//        double w_proc = w * 1.0 / scale;
//        double h_proc = h * 1.0 / scale;
//        Size temp = new Size(w_proc,h_proc);
//        Imgproc.resize(img, img_proc,temp);
        Mat img_proc = img;
        Mat img_dis = img_proc.clone();
        //扔给canny处理
        Mat gray= new Mat();
        Mat canny = new Mat();
        Imgproc.cvtColor(img_proc,gray,Imgproc.COLOR_BGR2GRAY);
        canny =getCanny(gray);
        //霍夫直线检测
        Mat lines = new Mat();
        Vector<LINE> horizontals= new Vector<>();
        Vector<LINE> verticals= new Vector<>();
        //这里原来是对canny进行检测的,
        //Imgproc.HoughLinesP(img, lines, 1, Math.PI /180, 30,100,30);
        Imgproc.HoughLinesP(canny,lines,1,Math.PI /180,80,50,50);
        //行，这里发现了其实lines的size 有长和宽的double的输出，那就好写了,用他的height，而且lines里面可以用get方法取出四维的double数组
        Imgcodecs.imwrite("f:/44.jpg",canny);
        for(int i =0;i<(int)lines.size().height;i++)
        {
            double [] store_ver  = lines.get(i,0);
            double delta_x = store_ver[0]-store_ver[2];
            double delta_y = store_ver[1]-store_ver[3];
            LINE l = new LINE ( new Point(store_ver[0],store_ver[1]), new Point(store_ver[2],store_ver[3]) );
            // 用直线端点来判断是水平的还是竖直的
            if (Math.abs(delta_x) > Math.abs(delta_y)) {
                horizontals.add(l);
            }
            else {
                verticals.add(l);
            }
            Imgproc.line(img_dis,new Point(store_ver[0],store_ver[1]),new Point (store_ver[2],store_ver[3]),new Scalar(0,255,100),1);
            Imgcodecs.imwrite("f:/55.jpg",img_dis);

        }
        //到这里找完了所有的直线并且把他们扔到了两个vector horizontals和verticals 里面

        //这里debug将找出来的水平线和竖直线画到img_dis上面看看
//         Imgproc.line(img_dis,horizontals.elementAt(0)._p1, horizontals.elementAt(0)._p2, new Scalar(0, 255, 0), 2);
//         Imgproc.line(img_dis,horizontals.elementAt(horizontals.size() - 1)._p1, horizontals.elementAt(horizontals.size() - 1)._p2, new Scalar(0, 255, 0), 2);
//         Imgproc.line(img_dis,verticals.elementAt(0)._p1, verticals.elementAt(0)._p2, new Scalar(0, 255, 0), 2);
//         Imgproc.line(img_dis,verticals.elementAt(verticals.size() - 1)._p1, verticals.elementAt(verticals.size() - 1)._p2, new Scalar(0, 255, 0), 2);
//         Imgcodecs.imwrite("f:/77.jpg",img_dis);


        //查看是否horizontals和verticals 里面不满两条直线
//        if (horizontals.size() < 2) {
//            if (horizontals.size() == 0 || horizontals.elementAt(0)._center.y > h_proc / 2) {
//                horizontals.add(new  LINE(new Point(0, 0), new Point(w_proc - 1, 0)));
//            }
//            if (horizontals.size() == 0 || horizontals.elementAt(0)._center.y <= h_proc / 2) {
//                horizontals.add(new LINE(new  Point(0, h_proc - 1), new  Point(w_proc - 1, h_proc - 1)));
//            }
//        }
//        if (verticals.size() < 2) {
//            if (verticals.size() == 0 || verticals.elementAt(0)._center.x > w_proc / 2) {
//                verticals.add(new  LINE(new  Point(0, 0), new  Point(0, h_proc - 1)));
//            }
//            if (verticals.size() == 0 || verticals.elementAt(0)._center.x <= w_proc / 2) {
//                verticals.add(new  LINE( new Point(w_proc - 1, 0), new Point(w_proc - 1, h_proc - 1)));
//            }
//        }

        //排序找出最上和最下的直线
        Collections.sort(horizontals,new cmp_y());
        Collections.sort(verticals,new cmp_x());

        //这里开始仿射变换

        int w_a4 = 1654,h_a4 = 2339;
        Mat dst = new Mat(h_a4, w_a4,CvType.CV_8UC3);//这里要改类型

        //先写最后的四个点
        Point dst_points[] = new Point[4];
        Point img_points[] = new Point[4];
        dst_points[0] = new Point (0,0);
        dst_points[1] = new Point (w_a4 - 1, 0);
        dst_points[2] = new Point (0, h_a4 - 1);
        dst_points[3] = new Point (w_a4 - 1, h_a4 - 1);
        //再写图里交点的四个点
        img_points[0]= computeIntersect(horizontals.elementAt(0) , verticals.elementAt(0));
        img_points[1]= computeIntersect(horizontals.elementAt(0) , verticals.elementAt(verticals.size()-1));
        img_points[2]= computeIntersect(horizontals.elementAt(horizontals.size()-1) , verticals.elementAt(0));
        img_points[3]= computeIntersect(horizontals.elementAt(horizontals.size()-1) , verticals.elementAt(verticals.size()-1));

        //这里看下四个角
        for(int i =0;i<4;i++)
        {
            System.out.print( img_points[i].x);
            System.out.print("   ");
            System.out.print( img_points[i].y);
            System.out.print("\r");

        }

        //这里debug看下画出来的四个点
//         Imgproc.circle(img_dis,img_points[0],1,new Scalar(0, 255, 0),2);
//         Imgproc.circle(img_dis,img_points[1],1,new Scalar(0, 255, 0),2);
//         Imgproc.circle(img_dis,img_points[2],1,new Scalar(0, 255, 0),2);
//         Imgproc.circle(img_dis,img_points[3],1,new Scalar(0, 255, 0),2);
//         Imgcodecs.imwrite("f:/77.jpg",img_dis);

        //把找出来的点缩放回去
//        for(int i =0;i< 4;i++)
//        {
//            img_points[i].x *= scale;
//            img_points[i].y *= scale;
//        }

        //这里debug看看画在原图上对不对
//         Imgproc.circle(img,img_points[0],1,new Scalar(0, 255, 0),2);
//         Imgproc.circle(img,img_points[1],1,new Scalar(0, 255, 0),2);
//         Imgproc.circle(img,img_points[2],1,new Scalar(0, 255, 0),2);
//         Imgproc.circle(img,img_points[3],1,new Scalar(0, 255, 0),2);
//         Imgcodecs.imwrite("f:/88.jpg",img);


//        Mat img_pts = new Mat (4,1,CvType.CV_32FC2);
//        Mat dst_pts = new Mat (4,1,CvType.CV_32FC2);
//        img_pts.put((int)img_points[0].x,(int)img_points[0].y,
//                (int)img_points[1].x,(int)img_points[1].y,
//                (int)img_points[2].x,(int)img_points[2].y,
//                (int)img_points[3].x,(int)img_points[3].y);
//
//        dst_pts.put((int)dst_points[0].x,(int)dst_points[0].y,
//                (int)dst_points[1].x,(int)dst_points[1].y,
//                (int)dst_points[2].x,(int)dst_points[2].y,
//                (int)dst_points[3].x,(int)dst_points[3].y);


        //这里最后都是要把4个点装到一个mat里面，那就不需要先用vector把他装起来了；
        MatOfPoint2f img_pts = new MatOfPoint2f(
                img_points[0],
                img_points[1],
                img_points[2],
                img_points[3]);

        MatOfPoint2f dst_pts = new MatOfPoint2f(
                dst_points[0],
                dst_points[1],
                dst_points[2],
                dst_points[3]);
        //变换矩阵
        Mat transmtx = Imgproc.getPerspectiveTransform(img_pts,dst_pts);
        //仿射变换
        Imgproc.warpPerspective(img, dst, transmtx, dst.size());



        //这里调试看看dst的样子
        Imgcodecs.imwrite("f:/66.jpg",dst);

        //ok,到这里将dst转成base64，return出去
        String res ;
        res = mat2base(dst,".jpg");
        return res;

    }

    public static void main(String[] args) {

        Mat kk =Imgcodecs.imread("f:/26.jpg");
        String base_da = mat2base(kk,".jpg");
        String tes =  scan(base_da);

        // Mat pp = base2mat(tes);
        //Imgcodecs.imwrite("f:/333.jpg",pp);

        //Mat dst =  base2mat(base_da) ;
        //Imgcodecs.imwrite("f:/99.jpg",dst);
    }


}
