package dabai;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.util.*;
public class cut_pic {

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
                in = cut_pic.class.getClassLoader().getResourceAsStream("libs/" + libFullName);
                if (in == null)
                    in = cut_pic.class.getResourceAsStream(libFullName);
                cut_pic.class.getResource(libFullName);
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


    public  static String scan (String base64_data){

        //想解决getrotationmatrix2d 产生的bug问题，原因其实就是虽然原图跟旋转图的矩形中心一样，
        //但是矩形的长和宽已经不一样了，要重新来再一次找contour，求contour的外接矩形，
        // 这个就是ROI，其实以前我做过这样的事情的。
        //现在其实就是三个思路，不用getrotationmatrix2d，用点对点映射的getPerspectiveTransform。这个问题是要判断点的上下左右关系，随便映射会镜像。
        //第二个思路是对binary的二值图做旋转，求他的外接矩形，这个可以试试
        //第三个思路是用getrotationmatrix2d，然后用旋转矩形的角度，求出旋转后的weigth和heigth。这个问题要考虑旋转矩形的角度问题。
        //还是要写java比较好，想着把这段代码，前面写一点通过颜色找阈值的方法，找出阈值图片的四个角点，这四个角点就是原来写好程序要的东西

        Mat img = base2mat(base64_data);
        Mat input = img.clone();
        Mat hsv_input = new Mat();
        Imgproc.cvtColor(input, hsv_input, Imgproc.COLOR_BGR2HSV);
        int width_step = (int) input.size().width / 5;
        int height_step = (int) input.size().height / 5;
        int[] h_count = new int[181];
        Arrays.fill(h_count, 0);
        for (int i = (int) input.size().width / 2 - width_step;
             i < input.size().width / 2 + width_step; i++)
            for (int j = (int) input.size().height / 2 - height_step;
                 j < input.size().height / 2 + height_step; j++) {
//                System.out.print(i);
//                System.out.print("     ");
//                System.out.print(j);
//                System.out.print("     ");
//                System.out.print((int)input.get(i,j)[0]);
//                System.out.print("\n");
                h_count[(int) hsv_input.get(j, i)[0]]++;
            }
//        for(int i =0;i<h_count.length;i++)
//        {
//            System.out.print(h_count[i]);
//            System.out.print("    ");
//            System.out.print(i);
//            System.out.print("\n");
//
//        }

//        //debug 看下h值的最大与最小
//        int min_val  =10000;
//        int max_val  = -1;
//        for(int i =0;i<(int)input.size().width;i++)
//            for(int j =0;j<(int)input.size().height;j++) {
//                System.out.print(i);
//                System.out.print("     ");
//                System.out.print(j);
//                System.out.print("     ");
//                System.out.print((int) hsv_input.get(j,i)[0]);
//                System.out.print("\n");
//                if ((int) hsv_input.get(j,i)[0]>max_val)
//                    max_val=(int) hsv_input.get(j,i)[0];
//                if((int) hsv_input.get(j,i)[0]<min_val)
//                    min_val = (int) hsv_input.get(j,i)[0];
//            }
//        System.out.print(min_val);System.out.print("max_val"); System.out.print(max_val);
//        //debug 看下h值的最大与最小
        int count_max = -1;
        int count_index = 10;
        for (int i = 0 + 10; i < 181 - 10; i++)
        {
            if ((h_count[i - 5] + h_count[i - 4] + h_count[i - 3] + h_count[i - 2] +
                    h_count[i - 1] + h_count[i] + h_count[i + 1] + h_count[i + 2] +
                    h_count[i + 3] + h_count[i + 4] + h_count[i + 5]) > count_max)
            {
                count_index = i;
                count_max = (h_count[i - 5] + h_count[i - 4] + h_count[i - 3] + h_count[i - 2] +
                        h_count[i - 1] + h_count[i] + h_count[i + 1] + h_count[i + 2] +
                        h_count[i + 3] + h_count[i + 4] + h_count[i + 5]);
            }

        }
        int xiaxian;
        int shangxian;
        if (count_index - 15 < 0)
            xiaxian = 0;
        else xiaxian = count_index - 15;
        if (count_index + 15 > 180)
            shangxian = 181;
        else shangxian = count_index + 15;
        Mat output = new Mat(input.rows(), input.cols(), CvType.CV_8UC1);
        for (int i = 0; i < input.size().width; i++) {
            for (int j = 0; j < input.size().height; j++) {
                //cout << i << "     " << j << endl;
                if (hsv_input.get(j, i)[0] > xiaxian && hsv_input.get(j, i)[0] < shangxian)
                    output.put(j, i, 255);
                else output.put(j, i, 0);
            }
        }
        //这里还是不去做找角点的操作了把，直接把contour找出来，在问下熊总能不能单独判别身份证
        Mat bin_output = new Mat();
        Imgproc.threshold(output, bin_output, 0, 255, Imgproc.THRESH_OTSU);
        //做一个bin图像的备份来给后面的仿射变换用
        Mat bin_output_clone = bin_output.clone();
        List<MatOfPoint> contours = new LinkedList<>();
        Mat hierachy = new Mat();
        Imgproc.findContours(bin_output, contours, hierachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        int qq = 0;
        int max = -1;
        for (int i = 0; i < contours.size(); i++) {
            if (Imgproc.contourArea(contours.get(i)) > max) {
                max = (int) Imgproc.contourArea(contours.get(i));
                qq = i;
            }
        }
        //找到了最大的轮廓之后开始找最大矩形
        RotatedRect roc_rect = new RotatedRect();
        MatOfPoint2f rect_points = new MatOfPoint2f();
        rect_points.fromList(contours.get(qq).toList());
        roc_rect = Imgproc.minAreaRect(rect_points);
        //求这个roc_rect的四个顶点
        Point[] poins = new Point[4];
        roc_rect.points(poins);
        //将这四个点做一个仿射变换
        Mat transform_mat = Imgproc.getRotationMatrix2D(roc_rect.center, roc_rect.angle, 1.0);
        Mat rot_image = new Mat();
        Mat rot_bin = new Mat();
        Size out_size = input.size();
        Imgproc.warpAffine(input, rot_image, transform_mat, out_size);
        Imgproc.warpAffine(bin_output_clone, rot_bin, transform_mat, out_size);

        //debug
        //Imgcodecs.imwrite(write_bin, rot_bin);
        //debug

        List<MatOfPoint> contours_last = new LinkedList<>();
        Mat hierachy_last = new Mat();
        //找rot_bin的最大contour的最小的boundingrect，这个rect就是在rot_img上要去的ROI
        Imgproc.findContours(rot_bin, contours_last, hierachy_last, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        qq = 0;
        max = -1;
        for (int i = 0; i < contours_last.size(); i++) {
            if (Imgproc.contourArea(contours_last.get(i)) > max) {
                max = (int) Imgproc.contourArea(contours_last.get(i));
                qq = i;
            }
        }
        Rect bounding_rect = Imgproc.boundingRect(contours_last.get(qq));

//            System.out.println(countt);
//            System.out.println("    ");
//            System.out.println(bounding_rect.height);
//            System.out.println("    ");
//            System.out.println(bounding_rect.width);
//            System.out.println("    ");
//            System.out.println(bounding_rect.x);
//            System.out.println("    ");
//            System.out.println(bounding_rect.y);
//            System.out.println("    ");
//            System.out.print("\n");

        Mat ROI_IMG = new Mat(rot_image, bounding_rect);
        String res;
        res  = mat2base(ROI_IMG,".jpg");
        Imgcodecs.imwrite("f:/1_1.jpg",ROI_IMG);

        return res ;

    }

    public static void main(String[] args) {
//        Mat pp = Imgcodecs.imread("f:/1.jpg");
//        String kk = mat2base(pp,".jpg");
//        scan(kk);
    }

}



