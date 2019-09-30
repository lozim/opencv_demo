// cuttingPic.cpp : 此文件包含 "main" 函数。程序执行将在此处开始并结束。
//



#include "pch.h"
#include <opencv2/opencv.hpp>
#include <opencv2/highgui/highgui.hpp>


using namespace cv;
using namespace std;

//函数说明
//输入basse64（std::string)到scan函数里面，返回一个处理完的图片的base64(std::string)  格式为jpg，大小固定为1654*2339；

//这些是mat和base64的转换
static std::string base64Decode(const char* Data, int DataByte) {
	//解码表
	const char DecodeTable[] =
	{
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		62, // '+'
		0, 0, 0,
		63, // '/'
		52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // '0'-'9'
		0, 0, 0, 0, 0, 0, 0,
		0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
		13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // 'A'-'Z'
		0, 0, 0, 0, 0, 0,
		26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38,
		39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // 'a'-'z'
	};
	String strDecode;
	int nValue;
	int i = 0;
	while (i < DataByte) {
		if (*Data != '\r' && *Data != '\n') {
			nValue = DecodeTable[*Data++] << 18;
			nValue += DecodeTable[*Data++] << 12;
			strDecode += (nValue & 0x00FF0000) >> 16;
			if (*Data != '=') {
				nValue += DecodeTable[*Data++] << 6;
				strDecode += (nValue & 0x0000FF00) >> 8;
				if (*Data != '=') {
					nValue += DecodeTable[*Data++];
					strDecode += nValue & 0x000000FF;
				}
			}
			i += 4;
		}
		else {
			Data++;
			i++;
		}
	}
	

	//cout << strDecode << endl;
	std::string ll;

	return ll;



}


static std::string base64Encode(const unsigned char* Data, int DataByte) {
	//编码表
	const char EncodeTable[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	//返回值
	std::string strEncode;
	unsigned char Tmp[4] = { 0 };
	int LineLength = 0;
	for (int i = 0; i < (int)(DataByte / 3); i++) {
		Tmp[1] = *Data++;
		Tmp[2] = *Data++;
		Tmp[3] = *Data++;
		strEncode += EncodeTable[Tmp[1] >> 2];
		strEncode += EncodeTable[((Tmp[1] << 4) | (Tmp[2] >> 4)) & 0x3F];
		strEncode += EncodeTable[((Tmp[2] << 2) | (Tmp[3] >> 6)) & 0x3F];
		strEncode += EncodeTable[Tmp[3] & 0x3F];
		if (LineLength += 4, LineLength == 76) { strEncode += "\r\n"; LineLength = 0; }
	}
	//对剩余数据进行编码
	int Mod = DataByte % 3;
	if (Mod == 1) {
		Tmp[1] = *Data++;
		strEncode += EncodeTable[(Tmp[1] & 0xFC) >> 2];
		strEncode += EncodeTable[((Tmp[1] & 0x03) << 4)];
		strEncode += "==";
	}
	else if (Mod == 2) {
		Tmp[1] = *Data++;
		Tmp[2] = *Data++;
		strEncode += EncodeTable[(Tmp[1] & 0xFC) >> 2];
		strEncode += EncodeTable[((Tmp[1] & 0x03) << 4) | ((Tmp[2] & 0xF0) >> 4)];
		strEncode += EncodeTable[((Tmp[2] & 0x0F) << 2)];
		strEncode += "=";
	}


	return strEncode;
}

//imgType 包括png bmp jpg jpeg等opencv能够进行编码解码的文件
static std::string Mat2Base64(const cv::Mat &img, std::string imgType) {
	//Mat转base64
	std::string img_data;
	std::vector<uchar> vecImg;
	std::vector<int> vecCompression_params;
	vecCompression_params.push_back(CV_IMWRITE_JPEG_QUALITY);
	vecCompression_params.push_back(90);
	imgType = "." + imgType;
	cv::imencode(imgType, img, vecImg, vecCompression_params);
	img_data = base64Encode(vecImg.data(), vecImg.size());
	return img_data;
}


static cv::Mat Base2Mat(std::string &base64_data) {
	cv::Mat img;
	std::string s_mat;
	
	
	s_mat = base64Decode(base64_data.data(), base64_data.size());
	std::vector<char> base64_img(s_mat.begin(), s_mat.end());
	img = cv::imdecode(base64_img, CV_LOAD_IMAGE_COLOR);
	return img;
}
//mat和base64的转换


//预处理
void getCanny(Mat gray, Mat &canny) {
	Mat thres;
	double high_thres = threshold(gray, thres, 0, 255, CV_THRESH_BINARY | CV_THRESH_OTSU), low_thres = high_thres * 0.5;
	Canny(gray, canny, low_thres, high_thres);
}
//写一个class容易找到中心point
struct Line {
	Point _p1;
	Point _p2;
	Point _center;

	Line(Point p1, Point p2) {
		_p1 = p1;
		_p2 = p2;
		_center = Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
	}
};

//写一个函数给sort来对比
bool cmp_y(const Line &p1, const Line &p2) {
	return p1._center.y < p2._center.y;
}

bool cmp_x(const Line &p1, const Line &p2) {
	return p1._center.x < p2._center.x;
}

//算两条线的交点
Point2f computeIntersect(Line l1, Line l2) {
	int x1 = l1._p1.x, x2 = l1._p2.x, y1 = l1._p1.y, y2 = l1._p2.y;
	int x3 = l2._p1.x, x4 = l2._p2.x, y3 = l2._p1.y, y4 = l2._p2.y;
	if (float d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)) {
		Point2f pt;
		pt.x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
		pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
		return pt;
	}
	return Point2f(-1, -1);
}

std::string scan(std::string  base64_data) {

	//生成img_pro img_dis
	Mat img = Base2Mat(base64_data);
	clock_t startTime, endTime;
	startTime = clock();//计时开始
	Mat img_proc;
	int w = img.size().width, h = img.size().height, min_w = 200;
	double scale = min(10.0, w * 1.0 / min_w);
	int w_proc = w * 1.0 / scale, h_proc = h * 1.0 / scale;
	resize(img, img_proc, Size(w_proc, h_proc));
	Mat img_dis = img_proc.clone();


	//扔去getcanny做预处理 ，可以在预处理里面加一些findcontour
	Mat gray, canny;
	cvtColor(img_proc, gray, CV_BGR2GRAY);
	getCanny(gray, canny);

	//霍夫直线检测
	vector<Vec4i> lines;
	vector<Line> horizontals, verticals;
	HoughLinesP(canny, lines, 1, CV_PI / 180, w_proc / 3, w_proc / 3, 20);
	for (size_t i = 0; i < lines.size(); i++) {
		Vec4i v = lines[i];
		double delta_x = v[0] - v[2], delta_y = v[1] - v[3];
		Line l(Point(v[0], v[1]), Point(v[2], v[3]));
		// 直线端点来判断是水平的还是竖直的
		if (fabs(delta_x) > fabs(delta_y)) {
			horizontals.push_back(l);
		}
		else {
			verticals.push_back(l);
		}
		// 画图
		//if (debug)
			//line(img_proc, Point(v[0], v[1]), Point(v[2], v[3]), Scalar(0, 0, 255), 1, CV_AA);
	}

	// 若不满两条线
	if (horizontals.size() < 2) {
		if (horizontals.size() == 0 || horizontals[0]._center.y > h_proc / 2) {
			horizontals.push_back(Line(Point(0, 0), Point(w_proc - 1, 0)));
		}
		if (horizontals.size() == 0 || horizontals[0]._center.y <= h_proc / 2) {
			horizontals.push_back(Line(Point(0, h_proc - 1), Point(w_proc - 1, h_proc - 1)));
		}
	}
	if (verticals.size() < 2) {
		if (verticals.size() == 0 || verticals[0]._center.x > w_proc / 2) {
			verticals.push_back(Line(Point(0, 0), Point(0, h_proc - 1)));
		}
		if (verticals.size() == 0 || verticals[0]._center.x <= w_proc / 2) {
			verticals.push_back(Line(Point(w_proc - 1, 0), Point(w_proc - 1, h_proc - 1)));
		}
	}
	// 排序找出最上和最下的直线
	sort(horizontals.begin(), horizontals.end(), cmp_y);
	sort(verticals.begin(), verticals.end(), cmp_x);
	// 画图
	//if (debug) {
		//line(img_proc, horizontals[0]._p1, horizontals[0]._p2, Scalar(0, 255, 0), 2, CV_AA);
		//line(img_proc, horizontals[horizontals.size() - 1]._p1, horizontals[horizontals.size() - 1]._p2, Scalar(0, 255, 0), 2, CV_AA);
		//line(img_proc, verticals[0]._p1, verticals[0]._p2, Scalar(255, 0, 0), 2, CV_AA);
		//line(img_proc, verticals[verticals.size() - 1]._p1, verticals[verticals.size() - 1]._p2, Scalar(255, 0, 0), 2, CV_AA);
	//}

	//仿射变换

	//输出定成一个固定尺寸
	int w_a4 = 1654, h_a4 = 2339;
	//int w_a4 = 595, h_a4 = 842;
	Mat dst = Mat::zeros(h_a4, w_a4, CV_8UC3);


	//用vector将四个点全部装起来(上左，上右，下左，下右）
	vector<Point2f> dst_pts, img_pts;
	dst_pts.push_back(Point(0, 0));
	dst_pts.push_back(Point(w_a4 - 1, 0));
	dst_pts.push_back(Point(0, h_a4 - 1));
	dst_pts.push_back(Point(w_a4 - 1, h_a4 - 1));

	//用vector将四个点全部装起来(上左，上右，下左，下右）
	img_pts.push_back(computeIntersect(horizontals[0], verticals[0]));//最上的横线和最左的竖线
	img_pts.push_back(computeIntersect(horizontals[0], verticals[verticals.size() - 1]));//最上的横线和最右的竖线
	img_pts.push_back(computeIntersect(horizontals[horizontals.size() - 1], verticals[0]));//最下的横线和最左的竖线
	img_pts.push_back(computeIntersect(horizontals[horizontals.size() - 1], verticals[verticals.size() - 1]));//最下的横线和最右的竖线

	// 将找出的点缩放回去
	for (size_t i = 0; i < img_pts.size(); i++) {
		// 画图
		//if (debug) {
	/*	circle(img_proc, img_pts[i], 10, Scalar(255, 255, 0), 3);*/
		//}
		img_pts[i].x *= scale;
		img_pts[i].y *= scale;
	}

	// 变换矩阵
	Mat transmtx = getPerspectiveTransform(img_pts, dst_pts);

	// 放射变换
	warpPerspective(img, dst, transmtx, dst.size());

	// 保存图片
	//将dst转换成base64来输出
	std::string res;
	res = Mat2Base64(dst, "jpg");


	endTime = clock();//计时结束
	cout << "程序耗时: " << (double)(endTime - startTime) / CLOCKS_PER_SEC << "s" << endl;

	// 画图
	//if (debug) {
	//namedWindow("dst", CV_WINDOW_KEEPRATIO);
	//imshow("src", img_dis);
	//imshow("canny", canny);
	//imshow("img_proc", img_proc);
	//imshow("dst", dst);
	//waitKey(0);
	//}

	return res;
}



int main(int argc, char** argv) {
	std::string input;
	ifstream infile;
	std::string file = "f:/1.txt";
	infile.open(file.data());   //将文件流对象与文件连接起来 
	assert(infile.is_open());   //若失败,则输出错误消息,并终止程序运行 
	char c;
	while (!infile.eof())
	{

		infile >> c;
		input.push_back(c);
	}
	infile.close();
	input.pop_back();




	//std::string input;
	scan(input);
	return 0;
}




