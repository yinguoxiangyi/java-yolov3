package com.dll;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.bytedeco.javacv.*;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.librealsense.frame;
import org.bytedeco.librealsense.intrinsics;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.indexer.*;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.*;
import org.bytedeco.opencv.opencv_calib3d.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_calib3d.*;
import static org.bytedeco.opencv.global.opencv_objdetect.*;

import org.opencv.core.Core;
import org.opencv.highgui.HighGui;
import org.opencv.img_hash.Img_hash;


import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.ptr.IntByReference;

import lombok.extern.slf4j.Slf4j;



public class DllMain {

//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		Smoother smoother = new Smoother();
//		smoother.smooth("F:\\work\\java_ws\\dllUseDemo2\\test.jpg");
//				
//	}
	public static OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
	
	public static int curFrameCount = 0;
	
	public static int totalFrameCount = 10;
	
	public static List<List<Box>> detectFramesBoxesList = new ArrayList<List<Box>>();
	
	public static Set<Integer>  needObSet = new HashSet<Integer>();
	
	static {
		//[2,3,4,6,8] # 从1开始 ；2:bicycle3:car4:motorbike6:bus8:truck
		needObSet.add(2);
		needObSet.add(3);
		needObSet.add(4);
		needObSet.add(6);
		needObSet.add(8);
		
	}
	
//	public static int isInitModel = CLibrary.INSTANCE.init("F:\\svnWork\\trunk\\darknetCdemo\\cfg\\yolov3.cfg", "F:\\svnWork\\trunk\\darknetCdemo\\yolov3.weights",0);
	
	public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary)
            Native.load("yolo_cpp_dll",
                                CLibrary.class);

        int add(int a, int b);
        
        int init(String configurationFilename, String weightsFilename, int gpuId);
        
        Box.ByReference data_test(int rows,int cols,int type,byte[] data,int step,int channel, IntByReference count);
        
    }
	
	public static int getUnsignedByte (byte data){      //将data字节型数据转换为0~255 (0xFF 即BYTE)。
        return data&0x0FF ;
     }
	
	public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte)((i >> 24) & 0xFF);
        result[1] = (byte)((i >> 16) & 0xFF);
        result[2] = (byte)((i >> 8) & 0xFF);
        result[3] = (byte)(i & 0xFF);
        return result;
    }
	
	public static long readUnsignedInt(byte[] bytes) {
        long b0 = ((long) (bytes[0] & 0xff));
        long b1 = ((long) (bytes[1] & 0xff)) << 8;
        long b2 = ((long) (bytes[2] & 0xff)) << 16;
        long b3 = ((long) (bytes[3] & 0xff)) << 24;
        return (long) (b0 | b1 | b2 | b3);
    }
	
	public int initModel() {
		int isInit = CLibrary.INSTANCE.init("F:\\svnWork\\trunk\\darknetCdemo\\cfg\\yolov3.cfg", "F:\\svnWork\\trunk\\darknetCdemo\\yolov3.weights",0);
		return isInit;
	}
	
	
	public int union(Box au, Box bu, int area_intersection) {
		int area_a = (au.xmax - au.xmin) * (au.ymax - au.ymin);
		int area_b = (bu.xmax - bu.xmin) * (bu.ymax - bu.ymin);
		int area_union = area_a + area_b - area_intersection;
		return area_union;
	}			
	
	public int intersection(Box ai, Box bi) {
		int x = Math.max(ai.xmin, bi.xmin);
		int y = Math.max(ai.ymin, bi.ymin);
		int w = Math.min(ai.xmax, bi.xmax) - x;
		int h = Math.min(ai.ymax, bi.ymax) - y;
		if (w < 0 || h < 0) {
			return 0;
		}
		return w*h;
	}	
	/*
	 * 
	 * 
	 * */
	public List<Box> filterOb(Box[] boxes, Set<Integer> idSet){
		List<Box> boxesList = new ArrayList<Box>();
		for(int i = 0; i < boxes.length; i++ ) {
			if (idSet.contains(boxes[i].obj_id)) {
				boxesList.add(boxes[i]);
			}
		}
		return boxesList;
	}
	
	public List<List<Box>> reId(List<List<Box>> totalFramesBoxes) {
		Integer carNum = 0;
		HashMap<Integer, List<String>> carIdHashMap = new HashMap<Integer,  List<String>>();
		for(int frameId = 0; frameId < totalFramesBoxes.size(); frameId++ ) {
			List<Box> singleFrameBoxes = totalFramesBoxes.get(frameId);
			for(int boxId = 0; boxId < singleFrameBoxes.size(); boxId++ ) {
				String carId = frameId + "-" + boxId;
				if ( ! carIdHashMap.isEmpty()) {
					List<Float> iouAvgList = new ArrayList<Float>();
					float max_iou_avg = 0.0f;
		            int max_iou_avg_idx  = 0;
		            
					for (Integer carNum_i : carIdHashMap.keySet()) {
						List<Float> iouList = new ArrayList<Float>();
						float iouSum = 0.0f;
						for (String carId_i : carIdHashMap.get(carNum_i)) {  
							int frameId_tmp = Integer.parseInt(carId_i.split("-")[0]);
							int boxId_tmp = Integer.parseInt(carId_i.split("-")[1]);
							float iou = iou(totalFramesBoxes.get(frameId).get(boxId), totalFramesBoxes.get(frameId_tmp).get(boxId_tmp));
							iouList.add(iou);
							iouSum += iou;
					     }
						
						float iouAvg = iouSum/((float) iouList.size());
						iouAvgList.add(iouAvg);
						if (iouAvg > max_iou_avg) {
							max_iou_avg = iouAvg;
							max_iou_avg_idx = iouAvgList.size() - 1;
						}
						
				    }
					if (max_iou_avg > 0.8f) {
						carIdHashMap.get(max_iou_avg_idx).add(carId);
					}else {
						List<String> carIdList = new ArrayList<String>();
						carIdList.add(carId);
						carIdHashMap.put(carNum, carIdList);
						carNum += 1;
					}
				}else {
					List<String> carIdList = new ArrayList<String>();
					carIdList.add(carId);
					carIdHashMap.put(carNum, carIdList);
					carNum += 1;
				}
					
			}
		}
		
		for (Integer carNum_i : carIdHashMap.keySet()) {
			// 警报类别: 0无警报 1杂物占用 2车辆占道 3无人警报    
	        int is_warning = 0;
	        List<String> carIdList = carIdHashMap.get(carNum_i);
	        if (((float)(carIdList.size()) >= ((float)(totalFramesBoxes.size()))*0.8f) && true) {// 计算每辆车的中心位置是不是在车道区域
	        	is_warning = 2;
	        }
	        for (String carId_i : carIdList) {  
				int frameId_tmp = Integer.parseInt(carId_i.split("-")[0]);
				int boxId_tmp = Integer.parseInt(carId_i.split("-")[1]);
				totalFramesBoxes.get(frameId_tmp).get(boxId_tmp).is_warning = is_warning;
		     }
	        
			
	    }
		return totalFramesBoxes;
	}
	
	public float iou(Box a, Box b) {
		// a and b should be (x1,y1,x2,y2)

		if ( (a.xmin >= a.xmax) || (a.ymin >= a.ymax) || (b.xmin >= b.xmax) || (b.ymin >= b.ymax))
			{return 0.0f;}

		int area_i = intersection(a, b); //求交集
		int area_u = union(a, b, area_i); //求并集

		return ((float) area_i) / ((float) area_u + 0.000001f);
	}			
	public int detectOccupy(Box[] boxes, HashSet<Integer> idSet) {
		return 1;
	}
	public Frame detectFrame(Frame frame) {
		Mat image = converter.convert(frame);
		int npixels0 = (int) ((int) image.total() * image.elemSize());
		byte[] pixels0 = new byte[(int)npixels0];
		BytePointer bytePointer = image.data();
		bytePointer.get(pixels0, 0, npixels0);
		
		IntByReference bbox_countByReference = new IntByReference();
		Box.ByReference boxesByReference = new Box.ByReference();
		long sta = System.currentTimeMillis();
		boxesByReference = CLibrary.INSTANCE.data_test(image.rows(),image.cols(),image.type(),pixels0,(int)image.step1(),image.channels(), bbox_countByReference);
		System.out.println("ms: " + (System.currentTimeMillis() - sta) );
		int bbox_count = bbox_countByReference.getValue();
		Box[] boxes = new Box[bbox_count];
		boxes = (Box[]) boxesByReference.toArray(bbox_count);
		//过滤box
		List<Box> filterBoxs = filterOb(boxes, needObSet);
		for(Box box : filterBoxs  ) {
			System.out.println("xmin:" + box.xmin + " ");
			rectangle(image, new Point(box.xmin, box.ymin), new Point(box.xmax, box.ymax), Scalar.RED, 3, CV_AA, 0);
		}
		if(detectFramesBoxesList.size() != totalFrameCount) {
			detectFramesBoxesList.add(filterBoxs);
		}else {
			detectFramesBoxesList = reId(detectFramesBoxesList);
			for(Box box : detectFramesBoxesList.get(totalFrameCount - 1)  ) {
//				System.out.println("xmin:" + box.xmin + " ");
				rectangle(image, new Point(box.xmin, box.ymin), new Point(box.xmax, box.ymax), Scalar.GREEN, 5, CV_AA, 0);
				if(box.is_warning != 0) {
					putText(image, "Warning", new Point(box.xmin, box.ymin), 3, 2.0, Scalar.RED);
				}else {
					putText(image, "Normal", new Point(box.xmin, box.ymin), 3, 2.0, Scalar.BLUE);
				}
				
			}
			
			detectFramesBoxesList.clear();
		}
		
		
		//显示图片
        Frame imageFrame = converter.convert(image);
        return imageFrame;
	}
	
	public void showFrames(String winTitle, FrameGrabber grabber) throws FrameGrabber.Exception, InterruptedException {
        double fps = grabber.getFrameRate();
        int interval =  (int) (1000.0/fps);
        int count = 0;
        int frame_itv = (int)(0.5*fps);
        System.out.println("fps is " + fps +" ");
		CanvasFrame canvas = new CanvasFrame(winTitle,1);//新建一个窗口
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);
        while (true) {
            if (!canvas.isVisible()) {
                break;
            }
            Frame frame = grabber.grab();
            // detect
            count += 1 ;   
            if (count%frame_itv==0) {
            	frame = detectFrame(frame);
                canvas.showImage(frame);
                Thread.sleep(interval);//50毫秒刷新一次图像
            }
            
            
        }
    }
	
	public void showSingleImage(Mat image) throws InterruptedException  {
		CanvasFrame canvas = new CanvasFrame("image",1);//新建一个窗口
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);
        List<Box> boxs = extractContourBox(image, 1500);
        Frame frame = converter.convert(image);
        canvas.showImage(frame);
        Thread.sleep(1000);//50毫秒刷新一次图像
    }
	
	public void testVideoPlay(String videoPath) throws InterruptedException, FrameGrabber.Exception {
		OpenCVFrameGrabber grabber = OpenCVFrameGrabber.createDefault(videoPath);
        grabber.setImageWidth(1280);
        grabber.setImageHeight(720);
        grabber.start();   //开始获取摄像头数据
        showFrames("Video", grabber);
        grabber.stop();
        grabber.close();
    }
	
	public void imgeDetect(String[] args) {
		//初始调用测试
//		int c = CLibrary.INSTANCE.add(5, 11);
		
		//图像数据传入测试
		Mat image = imread("F:\\work\\java_ws\\dllUseDemo2\\dog.jpg");
		
		
        //初始化方法调用测试
//		int c = CLibrary.INSTANCE.init("D:\\work\\tmp_done\\c#\\ConsoleApplication1\\ConsoleApplication1\\bin\\x64\\Debug\\models\\yolov3-orange_test_orange_data3_he.cfg", "D:\\work\\tmp_done\\c#\\ConsoleApplication1\\ConsoleApplication1\\bin\\x64\\Debug\\models\\yolov3-orange_best_1.weights",0);
		int c = CLibrary.INSTANCE.init("F:\\svnWork\\trunk\\darknetCdemo\\cfg\\yolov3.cfg", "F:\\svnWork\\trunk\\darknetCdemo\\yolov3.weights",0);
		
		int npixels0 = (int) ((int) image.total() * image.elemSize());
		byte[] pixels0 = new byte[(int)npixels0];
		BytePointer bytePointer = image.data();
		bytePointer.get(pixels0, 0, npixels0);
		IntByReference bbox_countByReference = new IntByReference();
		Box.ByReference boxesByReference = new Box.ByReference();
		boxesByReference = CLibrary.INSTANCE.data_test(image.rows(),image.cols(),image.type(),pixels0,(int)image.step1(),image.channels(), bbox_countByReference);
		int bbox_count = bbox_countByReference.getValue();
		System.err.println(getUnsignedByte(pixels0[0]));
		System.err.println(c);
		System.err.println(bbox_count);
		Box[] boxes = new Box[bbox_count];
		boxes = (Box[]) boxesByReference.toArray(bbox_count);
		Box box;
		
		for(int i = 0; i < bbox_count; i++ ) {
			box = boxes[i] ;
//			log.info()
//			byte[] tmpByte = intToByteArray(box.x);
//			long x =  readUnsignedInt(tmpByte);
//			box.x = box.x& 0xffffffffl;
//			box.y = box.y& 0xffffffffl;
//			box.w = box.w& 0xffffffffl;
//			box.h = box.h& 0xffffffffl;
//			System.out.println("x:" + box.x + ", y:" + box.y + ", w:" + box.w + ", h:" + box.h );
			System.out.println("xmin:" + box.xmin + " ");
			rectangle(image, new Point(box.xmin, box.ymin), new Point(box.xmax, box.ymax), Scalar.RED, 5, CV_AA, 0);
//			System.out.println(box.ymin);
//			System.out.println(box.type);
		}
		
		//显示图片
		
		OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
		CanvasFrame canvas = new CanvasFrame("img");//新建一个窗口
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);
        Frame imageFrame = converter.convert(image);
        canvas.showImage(imageFrame);	
//		System.err.println(image.type());
//		System.err.println(image.elemSize());
//		System.err.println(npixels0);
//		System.err.println(image.total());
//		System.err.println(image.rows());
//		System.err.println(image.cols());
//		System.err.println(image.channels());
//		
		//目标框传出测试

//		Box.ByReference boxes = new Box.ByReference();
//		Box[] boxes1 = new Box[2];
//		pmysStructur.ymax = 0.1f;
//		pmysStructur.xmax = 0.3f;
//		pmysStructur.ymin = 0.3f;
//		pmysStructur.xmin = 0.3f;
//		pmysStructur.type = 3;
		
//		IntByReference ib = new IntByReference();
//		DllTestApi.INSTANCE.setBox(box, 0.28f, 0.56f, 0.6f, 0.5f, 3);
//		System.out.println(box.xmin);
//		System.out.println(box.ymin);
		
//		System.out.println(DllTestApi.INSTANCE.addRef(pmysStructur));
//		
//		DllTestApi.myStructur.ByValue vmysStructur = new DllTestApi.myStructur.ByValue();
//		vmysStructur.a = 1;
//		vmysStructur.b = 3;		
//		
//		System.out.println(DllTestApi.INSTANCE.addNormal(vmysStructur));	
		
		//测试返回类型为结构体
//		boxes = DllTestApi.INSTANCE.getBox(0.28f, 0.56f, 0.6f, 0.5f, 3);
//		System.out.println(boxes.xmin);
//		System.out.println(boxes.ymin);
//		boxes1 = (Box[]) boxes.toArray(2);
//		System.out.println(boxes1[0].xmin);
//		System.out.println(boxes1[1].ymin);
//		System.out.println(boxes1[1].type);
    }

	public static List<Box> extractContourBox(Mat image, int contour_area){
		List<Box> boxs = new ArrayList<Box>();
		int height = image.rows();
        int width = image.cols();
		Mat grayImage = new Mat(height, width, CV_8UC1);
		Mat dst = new Mat(height, width, CV_8UC1);
		Mat gray_lwpCV = new Mat(height, width, CV_8UC1);
		Mat binary = new Mat(height, width, CV_8UC1);
		MatVector contours = new MatVector();
		cvtColor(image, grayImage, CV_BGR2GRAY);
		equalizeHist(grayImage ,dst);
		GaussianBlur(dst, gray_lwpCV, new Size(21, 21), 0.0);
		threshold(gray_lwpCV, binary, 127, 255, CV_THRESH_BINARY);
		findContours(binary, contours, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE);
		
		for(int i = 0; i < contours.size(); i++ ) {
			Mat contour  = contours.get(i);
			// 对于矩形区域，只显示大于给定阈值的轮廓，所以一些微小的变化不会显示。对于光照不变和噪声低的摄像头可不设定轮廓最小尺寸的阈值
	        if (contourArea(contour) < contour_area) {
	        	continue;
	        }
	        Rect rect = boundingRect(contour);
	        Box box = new Box();
	        box.initBox(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), 0.0f, 100, 0, 0, 0);
	        boxs.add(box);
	        rectangle(image, new Point(rect.x(), rect.y()), new Point(rect.x() + rect.width(), rect.y() + rect.height()), Scalar.RED, 3, CV_AA, 0);
		}
		
		return boxs;

	}
	
	public static void main(String[] args) throws Exception, InterruptedException {
		// TODO Auto-generated method stub
		DllMain dllMain = new DllMain();
		int ret = dllMain.initModel();
		dllMain.testVideoPlay("F:\\work\\temp\\jiazhouDetect\\darknetPyDemo\\video\\clip_video.mp4");
		
//		Mat image = imread("res/imgs/zawu.jpg");
//		dllMain.showSingleImage(image);
//		
//	    List<String> list =new ArrayList<String>();
//	    list.add("a");
//	    list.add("b");
//	    list.add("c");
	//1:通过索引遍历list
//	    for(int i=0;i<list.size();i++){
//	    System.err.println("1:"+list.get(i));   //err：输出换行
//	    System.out.print("2:"+list.get(i));    //out：输出不换行
//	    }

	//2：迭代器遍历
//	for(Iterator<String> it=list.iterator();it.hasNext();){
//	    String  str=it.next();
//	    System.out.println(str);
////	    it.remove();
//	}
//		
		//jacvmat 转化为 opencvmat
//		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//		org.opencv.core.Mat mat = new org.opencv.core.Mat(image.address());
//		HighGui.imshow(" 原图像",mat);
//		HighGui.waitKey(0);
		
	}
	
}
