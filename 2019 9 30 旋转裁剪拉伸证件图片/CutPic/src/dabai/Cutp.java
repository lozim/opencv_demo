package dabai;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.util.*;

public class Cutp  {

    static {
        try {
            loadLib("opencv_java320");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized static void loadLib(String libName) throws IOException {
        String systemType = System.getProperty("os.name");
        String libExtension = (systemType.toLowerCase().indexOf("win") != -1) ? ".dll" : ".so";
        String libFullName = libName + libExtension;
        String nativeTempDir = System.getProperty("java.io.tmpdir");
        InputStream in = null;
        BufferedInputStream reader = null;
        FileOutputStream writer = null;
        File extractedLibFile = new File(nativeTempDir + File.separator + libFullName);
        if (!extractedLibFile.exists()) {
            try {
                in = Cutp.class.getClassLoader().getResourceAsStream("libs/" + libFullName);
                if (in == null)
                    in = Cutp.class.getResourceAsStream(libFullName);
                Cutp.class.getResource(libFullName);
                reader = new BufferedInputStream(in);
                writer = new FileOutputStream(extractedLibFile);

                byte[] buffer = new byte[1024];

                while (reader.read(buffer) > 0) {
                    writer.write(buffer);
                    buffer = new byte[1024];
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null)
                    in.close();
                if (writer != null)
                    writer.close();
            }
        }
        System.load(extractedLibFile.toString());
    }
    //String file = System.getProperty("user.dir");
    //String file_name = file+"\\opencv_java320.dll";
    //System.load(file_name);


    private static String mat2base(Mat matrix, String fileExtension) {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(fileExtension, matrix, mob);
        byte[] byteArray = mob.toArray();
        //再在这里把byte数组变成base64就好了
        final Base64.Encoder encoder = Base64.getEncoder();
        final String encodedText = encoder.encodeToString(byteArray);
        return  encodedText;
    }

    private static  Mat base2mat (String base_data ){
        final Base64.Decoder decoder = Base64.getDecoder();
        byte [] bytearray =  decoder.decode(base_data);
        Mat mat = Imgcodecs.imdecode(new MatOfByte(bytearray), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
        return mat;

    }
    //预处理
    private static Mat getCanny (Mat gray){
        Mat res = new Mat();
        Mat thres =new Mat() ;
        double high_thres = Imgproc.threshold(gray, thres, 0, 255, Imgproc.THRESH_BINARY| Imgproc.THRESH_OTSU), low_thres = high_thres * 0.5;
        Imgproc.Canny(gray, res, low_thres, high_thres);
        return res;
    }
    //写一个class容易找到中心point
    private static class LINE{
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
    private static class cmp_y implements Comparator<LINE>{
        @Override
        public  int compare(final LINE p1, final LINE p2){
            //return  (int)(p1._center.y < p2._center.y);
            if(p1._center.y < p2._center.y)
                return 1;
            else return -1;

        }
    }
    private static class cmp_x implements Comparator<LINE>{
        @Override
        public int compare(final LINE p1, final LINE p2){
            //return p1._center.x < p2._center.x;
            if(p1._center.x < p2._center.x)
                return 1;
            else return -1;
        }
    }
    //算两条线的交点
    private static Point computeIntersect(LINE l1, LINE l2) {
        int x1 = (int)l1._p1.x, x2 = (int)l1._p2.x, y1 = (int)l1._p1.y, y2 = (int)l1._p2.y;
        int x3 = (int)l2._p1.x, x4 = (int)l2._p2.x, y3 = (int)l2._p1.y, y4 = (int)l2._p2.y;
        float d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4) ;
        Point pt = new Point();
        pt.x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
        pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
        return pt;
    }
    //主函数  有两部分，先是找最大轮廓面积的轮廓的外接矩形，旋转裁切  ，再对切完的图片找最外围的直线，四个角点来放射变换，拉伸消除拍照
    //带来的矩形拍出来不是矩形的问题。207-217行的修正 minarearect的左上点是负坐标的问题 脑袋一拍取了个*2.8，这样还是会有机会导致程序崩溃的
    public  static String scan (String base64_data){
        //findcontour
        Mat img = base2mat(base64_data);

        Mat input = img.clone();
        Mat gray = new Mat();
        Mat thres = new Mat();
        Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY);

        Mat canny = new Mat();
        double high_thres = Imgproc.threshold(gray, thres, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU), low_thres = high_thres * 0.5;
        Imgproc.Canny(gray, canny, low_thres, high_thres);
//        String gg ="f:/tt/"+number+"_5"+".jpg";
//        Imgcodecs.imwrite(gg, canny);
        //腐蚀膨胀没用的话，那就先find一波外围的contour吧
        Mat erzhi = canny.clone();



        //find contour
        List<MatOfPoint> contours = new LinkedList<>();
        Mat hierachy = new Mat();
        Imgproc.findContours(erzhi, contours, hierachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        //看一下contours是啥样子的
//        Mat empty = Mat.zeros(input.size(), CvType.CV_8UC3);
//        Imgproc.drawContours(empty, contours, -1, new Scalar(0, 255, 100), 3);
//        String yy= "f:/tt/"+number+"_4"+".jpg";
//        Imgcodecs.imwrite(yy,empty);



        int qq = 0;
        int  max= -1;
        for (int i = 0; i < contours.size(); i++) {
            if (Imgproc.contourArea(contours.get(i)) > max) {
                max= (int)Imgproc.contourArea(contours.get(i));
                qq = i;
            }
        }

        //String kk= "f:/tt/"+number+"_4"+".jpg";
        //Imgproc.drawContours(input,contours,qq,new Scalar(0,255,100));
        //Imgcodecs.imwrite(kk,input);
        //用一个矩形把他装起来
        RotatedRect roc_rect = new RotatedRect();
        MatOfPoint2f rect_points = new MatOfPoint2f();
        rect_points.fromList(contours.get(qq).toList());
        roc_rect = Imgproc.minAreaRect(rect_points);
        //求这个roc_rect的四个顶点
        Point[] poins = new Point[4];
        roc_rect.points(poins);
        //将这四个点做一个仿射变换
        Point center = roc_rect.center;

        int width = (int) roc_rect.size.width;
        int height = (int) roc_rect.size.height;
        Mat transform_mat = Imgproc.getRotationMatrix2D(roc_rect.center, roc_rect.angle, 1.0);
        Mat rot_image = new Mat();
        Size out_size = input.size();
        Imgproc.warpAffine(input, rot_image, transform_mat, out_size);


        //debug
        //Imgcodecs.imwrite("f:/14.jpg",rot_image);
//        System.out.print(width );
//        System.out.print("   ");
//        System.out.print(height );
//        System.out.print("   ");
//        System.out.print((int) center.x - (width / 2));
//        System.out.print("   ");
//        System.out.print((int) center.y - (height / 2));
        //debug

        Mat imgDesc = new Mat(width, height, CvType.CV_8UC3);
        int start_x;
        int start_y;
        int buchang_x=0;
        int buchang_y=0;

        if((int) center.x - (width / 2)<0) {
            start_x = 0;
            buchang_x=(int) ((center.x - (width / 2))*2.8);
        }
        else start_x=(int) center.x - (width / 2);

        if((int) center.y - (height / 2)<0){
            start_y=0;
            buchang_y=(int) ((center.y - (height / 2))*2.8);
        }
        else start_y=(int) center.y - (height / 2);


        //debug
//        System.out.print("   ");
//        System.out.print(start_x );
//        System.out.print("   ");
//        System.out.print(start_y );
//        System.out.print("   ");
//        System.out.print(rot_image.size().width);
//        System.out.print("   ");
//        System.out.print(rot_image.size().height);
//        System.out.print("   ");
//        System.out.print(buchang_x );
//        System.out.print("   ");
//        System.out.print(buchang_y );
//        System.out.print("\n");
        //debug

        Mat imgROI = new Mat(rot_image, new Rect(start_x, start_y, width+buchang_x, height+buchang_y));
        imgROI.copyTo(imgDesc);
        //Imgcodecs.imwrite(j4, imgDesc);
        //String bb= "f:/tt/"+number+"_2"+".jpg";
        //Imgcodecs.imwrite(bb,imgDesc);




        //Mat imgDesc = base2mat(base64_data);


        //这里开始原来的scan
        //做一些缩放

        Mat img_proc=imgDesc.clone();
        int w = (int)imgDesc.size().width;
        int h = (int)imgDesc.size().height;
        //min_w = 200;
//        double scale = Math.min(10.0, w * 1.0 / min_w);
//        double w_proc = w * 1.0 / scale;
//        double h_proc = h * 1.0 / scale;
//        Size temp = new Size(w_proc,h_proc);
//        Imgproc.resize(img, img_proc,temp);


        //扔给canny处理
        Mat gray_la= new Mat();
        Mat canny_la = new Mat();
        Imgproc.cvtColor(img_proc,gray_la,Imgproc.COLOR_BGR2GRAY);
        canny_la =getCanny(gray_la);

        //Imgcodecs.imwrite("f:/14.jpg",canny_la);


        //霍夫直线检测
        Mat lines = new Mat();
        Vector<LINE> horizontals= new Vector<>();
        Vector<LINE> verticals= new Vector<>();
        //这里原来是对canny进行检测的,
        //Imgproc.HoughLinesP(img, lines, 1, Math.PI /180, 30,100,30);
        Imgproc.HoughLinesP(canny_la,lines,1,Math.PI /180,(int)w/3,w/3,20);
        //行，这里发现了其实lines的size 有长和宽的double的输出，那就好写了,用他的height，而且lines里面可以用get方法取出四维的double数组
        //Imgcodecs.imwrite("f:/66.jpg",canny);
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
        }
        //到这里找完了所有的直线并且把他们扔到了两个vector horizontals和verticals 里面




        //查看是否horizontals和verticals 里面不满两条直线
        if (horizontals.size() < 2) {
            if (horizontals.size() == 0 || horizontals.elementAt(0)._center.y > h / 2) {
                horizontals.add(new  LINE(new Point(0, 0), new Point(w - 1, 0)));
            }
            if (horizontals.size() == 0 || horizontals.elementAt(0)._center.y <= h / 2) {
                horizontals.add(new LINE(new  Point(0, h - 1), new  Point(w - 1, h - 1)));
            }
        }
        if (verticals.size() < 2) {
            if (verticals.size() == 0 || verticals.elementAt(0)._center.x > w / 2) {
                verticals.add(new  LINE(new  Point(0, 0), new  Point(0, h - 1)));
            }
            if (verticals.size() == 0 || verticals.elementAt(0)._center.x <= w / 2) {
                verticals.add(new  LINE( new Point(w - 1, 0), new Point(w - 1, h - 1)));
            }
        }

        //这里debug将找出来的水平线和竖直线画到img_dis上面看看
//         Imgproc.line(imgDesc,horizontals.elementAt(0)._p1, horizontals.elementAt(0)._p2, new Scalar(0, 255, 110), 2);
//         Imgproc.line(imgDesc,horizontals.elementAt(horizontals.size() - 1)._p1, horizontals.elementAt(horizontals.size() - 1)._p2, new Scalar(0, 255, 110), 2);
//         Imgproc.line(imgDesc,verticals.elementAt(0)._p1, verticals.elementAt(0)._p2, new Scalar(0, 255, 110), 2);
//         Imgproc.line(imgDesc,verticals.elementAt(verticals.size() - 1)._p1, verticals.elementAt(verticals.size() - 1)._p2, new Scalar(0, 255, 110), 2);
//         Imgcodecs.imwrite("f:/16.jpg",imgDesc);

        //排序找出最上和最下的直线
        Collections.sort(horizontals,new cmp_y());
        Collections.sort(verticals,new cmp_x());

        //这里开始仿射变换

        int w_a4 = w,h_a4 = h;
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

        //这里debug看下画出来的四个点
//         Imgproc.circle(imgDesc,img_points[0],1,new Scalar(100, 255, 0),20);
//         Imgproc.circle(imgDesc,img_points[1],1,new Scalar(100, 255, 0),20);
//         Imgproc.circle(imgDesc,img_points[2],1,new Scalar(100, 255, 0),20);
//         Imgproc.circle(imgDesc,img_points[3],1,new Scalar(100, 255, 0),20);
//         Imgcodecs.imwrite("f:/15.jpg",imgDesc);

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
        Imgproc.warpPerspective(img_proc, dst, transmtx, dst.size());
        //这里调试看看dst的样子
        //Imgcodecs.imwrite("f:/13.jpg",dst);

        //ok,到这里将dst转成base64，return出去
        String res ;
        res = mat2base(dst,".jpg");
        return res;
    }

    public static void main(String[] args) {
//        String j1= "f:/im/";
//        String j2=".jpg";
//        String j3;
//        String j4;
//        for(int i = 1;i<31;i++) {
//            j3= "f:/im/"+i+"_1.jpg";
//            j4="f:/tt/"+i+"_3"+".jpg";
//            Mat kk = Imgcodecs.imread(j3);
//            String base_da = mat2base(kk, ".jpg");
//            String tes = scan(base_da,i);
//            Mat pp = base2mat(tes);
//            Imgcodecs.imwrite(j4, pp);
//        }
//
//        //Mat dst =  base2mat(base_da) ;
//        //Imgcodecs.imwrite("f:/99.jpg",dst);

    }


}
