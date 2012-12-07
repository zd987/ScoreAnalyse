package org.thu;



import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;

import umontreal.iro.lecuyer.gof.KernelDensity;
import umontreal.iro.lecuyer.probdist.EmpiricalDist;
import umontreal.iro.lecuyer.probdist.NormalDist;

/**
 * @author zd987
 *
 */
/**
 * @author zd987
 *
 */
public class Engine
{
	private double[] scoreArray = null;
	private double expectedValue = -1.0;
	private double squareDeviation = -1.0;
	private double standardDeviation = -1.0;
	private double skewness = -1.0;
	private double kurtosis = -1.0;
	private boolean isNormalDistribution = false;
	public Engine() {
	}
	
	/** 设置输入数据
	 * @param scoreArray 成绩数组
	 */
	public void setInputArray(double[] scoreArray) {
		this.scoreArray = scoreArray;
		analyse();
	}
	
	public boolean testNormalDistribution() {
		return isNormalDistribution;
	}
	
	//获取平均值
	public double getExpectedValue() {
		return this.expectedValue;
	}	
	
	//获取标准差
	public double getStandardDeviation() {
		return this.standardDeviation;
	}
	
	//获取方差
	public double getSquareDeviation() {
		return this.squareDeviation;
	}
	
	//获取偏度系数
	public double getSkewness() {
		/*
		 *  Skewness=0     分布形态与正态分布偏度相同
			Skewness>0     正偏差数值较大，为正偏或右偏。长尾巴拖在右边。
			Skewness<0     负偏差数值较大，为负偏或左偏。长尾巴拖在左边。
		 * */
		return skewness;
	}
	
	//获取峰度系数
	public double getKurtosis() {
		/*
		 *  Kurtosis=0       与正态分布的陡缓程度相同。
			Kurtosis>0       比正态分布的高峰更加陡峭——尖顶峰
			Kurtosis<0       比正态分布的高峰来得平台——平顶峰
		 * */
		return kurtosis;
	}
	
	private void analyse() {
		int i;
		double sum = 0.0, cm2, cm3, cm4, m3, m4, tmp, tmp2;
		for(i = 0; i < scoreArray.length; ++i) {
			sum += scoreArray[i];
		}
		expectedValue = sum / scoreArray.length;
		cm2 = 0.0;
		cm3 = 0.0;
		cm4 = 0.0;
		for(i = 0; i < scoreArray.length; ++i) {
			tmp = scoreArray[i] - expectedValue;
			tmp2 = tmp * tmp;
			cm2 += tmp2;
			cm3 += tmp2 * tmp;
			cm4 += tmp2 * tmp2;
		}
		squareDeviation = cm2 / scoreArray.length;
		m3 = cm3 / scoreArray.length;
		m4 = cm4 / scoreArray.length;
		standardDeviation = Math.sqrt(squareDeviation);
		skewness = m3 / (squareDeviation * standardDeviation);
		kurtosis = m4 / (squareDeviation * squareDeviation) - 3;
		double Ua = 1.9599639845400538;
		//这里是选用置信度a = 0.05，通过查标准正态分布表找到U(a / 2) = 1.96,来计算下面的正态分布检验
		//计算方法详见 参考文献： http://www.cnki.com.cn/Article/CJFDTotal-HJDZ199002013.htm
		if(Math.abs(skewness) <= Ua * Math.sqrt(6.0 / scoreArray.length) &&
				Math.abs(kurtosis) <= Ua * Math.sqrt(24.0 / scoreArray.length)) {
			isNormalDistribution = true;
		} else {
			isNormalDistribution = false;
		}
	}

	/**
	 * @param filePath 要保存png文件的绝对路径
	 * @param width 图片宽度，样例用选取的图片宽度为800
	 * @param height 图片高度，样例中选取的图片高度为500
	 * @param step 分数段的步长，可以指定[1, 100]之间的任意整数
	 * @param mergeBelow60 布尔型变量，来设置是否合并不及格分数段
	 * @param displayFittedCurve 布尔型变量，来设置是否需要显示拟合曲线
	 */
	public void createAnalyseChart(String filePath, int width, int height, 
			int step, boolean mergeBelow60, boolean displayFittedCurve) {
		if(step <= 0) {
			return;
		}
		int i, num = 0, start = 0, end = -1;
		if(!mergeBelow60) {
			num = 100 / step;
			end = 100 / step * step;
			start = 0;
		} else {
			num = 40 / step;
			end = 60 + 40 / step * step;
			start = 60 - step;
			++num;
		}
		if(end < 100) {
			++num;
			end += step;
		}
		HistogramDataset dataset = new HistogramDataset();
	    dataset.addSeries("分数段人数统计", scoreArray, num, start, end);
	    MyNumberAxis xAxis = new MyNumberAxis("成绩", start, step);
	    xAxis.setRange(new Range(start - 2, end + 2), true, true);
		NumberAxis yAxis = new NumberAxis("人数");
		yAxis.setAutoRangeIncludesZero(false);
		yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		XYBarRenderer.setDefaultShadowsVisible(false);
		XYBarRenderer renderer1 = new XYBarRenderer(0);
		renderer1.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator());
		renderer1.setBaseItemLabelsVisible(true);
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer1);
		
		/////////////////////////////
		if(displayFittedCurve) {
			double[] x = new double[801];
		    double dstep = 100.0 / 800;
		    for(i = 1; i <= 800; ++i) {
		    	x[i] = x[i - 1] + dstep;
		    }
		    double[] copyArray = scoreArray.clone();
		    Arrays.sort(copyArray);
		    EmpiricalDist ed = new EmpiricalDist(copyArray);
		    NormalDist nd = new NormalDist();
		    double[] y = KernelDensity.computeDensity(ed, nd, x);
		    double[][] data2 = new double[2][800];
	     	data2[0] = x;
		    data2[1] = y;
		    XYSeries s1 = new XYSeries("拟合曲线");
		    for(i = 0; i <= 800; ++i) {
		    	s1.add(x[i], y[i]);
		    }
		    XYSeriesCollection dataset2 = new XYSeriesCollection();
		    dataset2.addSeries(s1);
			plot.setDataset(1, dataset2);
			XYItemRenderer renderer2 = new StandardXYItemRenderer();
			plot.setRenderer(1, renderer2);
			NumberAxis localNumberAxis2 = new NumberAxis("概率密度");
			NumberFormat nf = NumberFormat.getPercentInstance();
			nf.setMaximumFractionDigits(2);
			nf.setMinimumFractionDigits(2);
		    localNumberAxis2.setNumberFormatOverride(nf);
		    plot.setRangeAxis(1, localNumberAxis2);
		    plot.mapDatasetToRangeAxis(1, 1);
			plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		}
		/////////////////////////////
		
		JFreeChart chart = new JFreeChart("成绩分布统计图",
			  JFreeChart.DEFAULT_TITLE_FONT, plot, true);

		String subTitle = String.format("期望：%1$.2f, 方差：%2$.2f, 偏度系数：%3$.2f, 峰度系数：%4$.2f", expectedValue, squareDeviation, skewness, kurtosis);
		TextTitle localTextTitle = new TextTitle(subTitle, new Font("SansSerif", Font.PLAIN, 12));
		chart.addSubtitle(localTextTitle);
		chart.getRenderingHints().put(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		Font labelFont = new Font("SansSerif", Font.TRUETYPE_FONT, 12);
		ValueAxis domainAxis = plot.getDomainAxis();
		domainAxis.setLabelFont(labelFont);// 轴标题
		domainAxis.setTickLabelFont(labelFont);// 轴数值
		TextTitle textTitle = chart.getTitle();
        textTitle.setFont(new Font("SansSerif", Font.BOLD, 25));
		Object columnNames[] = { "分数段", "人数"};
		Object rowData[][] = new Object[num][2];
		String low, high;
		for(i = 0; i < num; ++i) {
			if(i == 0) {
				low = "0";
			} else {
				low = Integer.toString(start + step * i);
			}
			if(i == num - 1) {
				high = Integer.toString(100);
			} else {
				high = Integer.toString(start + step * (i + 1) - 1);
			}rowData[i][0] = low + " ~ " + high;
		}
		for(i = 0; i < dataset.getItemCount(0); ++i) {
			rowData[i][1] = "" + dataset.getY(0, i).intValue();
		}
		ChartPanel panel = new ChartPanel(chart);
		JTable table = new JTable(rowData, columnNames);
		table.getTableHeader().setFont(labelFont);
	    JScrollPane scrollPane = new JScrollPane(table);
	    panel.setPreferredSize(new Dimension(width * 2 / 3, height));
	    scrollPane.setPreferredSize(new Dimension(width - width * 2 / 3, height));
	    JFrame frame = new JFrame();
	    frame.setSize(width, height);
	    frame.add(panel, BorderLayout.CENTER);
	    frame.add(scrollPane, BorderLayout.EAST);
	  //  frame.pack();
	    frame.setVisible(true);
	    BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); 
	    Graphics2D g = bi.createGraphics();
	    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
	    frame.paint(g);  //this == JComponent
	    g.dispose();
	    try{javax.imageio.ImageIO.write(bi,"png",new File(filePath));}catch (Exception e) {}
	    frame.dispose();
	}
	
	/** 
	 * @param filePath 要保存png文件的绝对路径
	 * @param width 图片宽度，样例用选取的图片宽度为500
	 * @param height 图片高度，样例中选取的图片高度为500
	 */
	public void createQQPlot(String filePath, int width, int height) {
		NormalDist nd = new NormalDist();
	    nd.setParams(this.expectedValue, this.standardDeviation);
	    double[] copyArray = scoreArray.clone();
	    Arrays.sort(copyArray);
	    double[] x2 = new double[copyArray.length];
	    int i;
	    double tmp;
	    XYSeries s1 = new XYSeries("Quantile-Quantile");
	    XYSeries s2 = new XYSeries("参考线");
	    for(i = 1; i <= x2.length; ++i) {
	    	tmp = (2 * i - 1);
	    	x2[i - 1] = nd.inverseF(tmp / (2 * x2.length));
	    	s1.add(copyArray[i - 1], x2[i - 1]);
	    	s2.add(copyArray[i - 1], copyArray[i - 1]);
	    }
	    XYSeriesCollection xyc = new XYSeriesCollection();
	    xyc.addSeries(s2);
	    xyc.addSeries(s1);
	    JFreeChart localJFreeChart = ChartFactory.createScatterPlot("正态分布检验Q-Q图", "X", "Y", xyc, PlotOrientation.VERTICAL, true, true, false);
	    XYPlot localXYPlot = (XYPlot)localJFreeChart.getPlot();
	    localXYPlot.setDomainPannable(true);
	    localXYPlot.setRangePannable(true);
	    XYLineAndShapeRenderer localXYLineAndShapeRenderer = new XYLineAndShapeRenderer();
	    localXYLineAndShapeRenderer.setSeriesLinesVisible(0, true);
	    localXYLineAndShapeRenderer.setSeriesShapesVisible(0, false);
	    localXYLineAndShapeRenderer.setSeriesLinesVisible(1, false);
	    localXYLineAndShapeRenderer.setSeriesShapesVisible(1, true);
	    localXYLineAndShapeRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
	    localXYLineAndShapeRenderer.setDefaultEntityRadius(6);
	    localXYPlot.setRenderer(localXYLineAndShapeRenderer);
	    Font labelFont = new Font("SansSerif", Font.PLAIN, 12);
		ValueAxis domainAxis = localXYPlot.getDomainAxis();
	//	domainAxis.setLabelFont(labelFont);// 轴标题
		domainAxis.setTickLabelFont(labelFont);// 轴数值
		TextTitle textTitle = localJFreeChart.getTitle();
        textTitle.setFont(new Font("SansSerif", Font.BOLD, 25));
        localXYLineAndShapeRenderer.setBaseLegendTextFont(labelFont);
        localJFreeChart.getRenderingHints().put(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
	    FileOutputStream fos_jpg = null;
		try
		{
			String chartName = filePath;
			fos_jpg = new FileOutputStream(chartName);

			// 将报表保存为png文件
			ChartUtilities.writeChartAsPNG(fos_jpg, localJFreeChart, width, height);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				fos_jpg.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static void test() throws FileNotFoundException {
	    /*
		ArrayList<Double> list = new ArrayList<Double>();
		FileReader fileReader =new FileReader("E:/score2.txt");
		Scanner scanner = new Scanner(fileReader);
		while(scanner.hasNext()) {
			list.add(scanner.nextDouble());
		}
		double[] arrayF = new double[list.size()];
		int i = 1;
		for(i = 0; i < list.size(); ++i){
			arrayF[i] = list.get(i);
		}
		double[] b = new double[100];
		Random r = new Random();
		for(i = 0; i < 100; ++i) {
			b[i] = r.nextGaussian();
		}
		*/
		double[] array = {84.000,70.000,79.000,91.000,87.000,84.000,74.000,79.000,90.000,85.000,84.000,72.000,88.000,84.000,89.000,79.000,81.000,82.000,77.000,82.000,87.000,83.000,82.000,69.000,74.000,84.000,90.000,79.000,77.000,72.000,74.000,90.000,89.000,80.000,81.000,82.000,86.000,87.000,91.000,89.000,89.000,66.000,88.000,64.000,84.000,75.000,82.000,89.000,86.000,75.000,85.000,90.000,88.000,85.000,89.000,64.000,78.000,84.000,90.000,86.000,75.000,84.000,79.000,80.000,82.000,80.000,92.000,86.000,90.000,69.000,71.000,74.000,87.000,90.000,89.000,65.000,76.000,86.000,80.000,56.000,90.000,85.000,91.000,66.000,75.000,71.000,86.000,79.000,90.000,65.000,81.000,85.000,81.000,70.000,90.000,83.000,67.000,71.000,82.000,72.000,83.000,82.000,80.000,87.000,79.000,85.000,82.000,82.000,78.000,84.000,86.000,68.000,84.000,85.000,89.000,85.000,89.000,88.000,88.000,85.000,90.000,75.000,90.000,86.000,83.000,85.000,84.000,78.000,69.000,86.000,78.000,83.000,85.000,80.000,86.000,52.000,87.000,68.000,89.000,87.000,88.000,90.000,75.000,88.000,87.000,90.000,86.000,76.000,92.000,83.000,83.000,86.000,87.000,82.000,67.000,86.000,74.000,83.000,90.000,83.000,86.000,84.000,88.000,85.000,64.000,57.000,88.000,87.000,71.000,84.000,81.000,89.000,81.000,73.000,88.000,78.000,83.000,70.000,82.000,84.000,90.000,80.000,91.000,57.000};
		
		int width = 800, height = 500;
		Engine e = new Engine();
		e.setInputArray(array);
		e.createAnalyseChart("D:/zd987.PERMANENT/Desktop/analyse.png", width, height, 10, false, true);
		e.createQQPlot("D:/zd987.PERMANENT/Desktop/QQ_plot.png", 500, 500);
	}
	
	public static void test1() {
		double[] array = {84.000,70.000,79.000,91.000,87.000,84.000,74.000,79.000,90.000,85.000,84.000,72.000,88.000,84.000,89.000,79.000,81.000,82.000,77.000,82.000,87.000,83.000,82.000,69.000,74.000,84.000,90.000,79.000,77.000,72.000,74.000,90.000,89.000,80.000,81.000,82.000,86.000,87.000,91.000,89.000,89.000,66.000,88.000,64.000,84.000,75.000,82.000,89.000,86.000,75.000,85.000,90.000,88.000,85.000,89.000,64.000,78.000,84.000,90.000,86.000,75.000,84.000,79.000,80.000,82.000,80.000,92.000,86.000,90.000,69.000,71.000,74.000,87.000,90.000,89.000,65.000,76.000,86.000,80.000,56.000,90.000,85.000,91.000,66.000,75.000,71.000,86.000,79.000,90.000,65.000,81.000,85.000,81.000,70.000,90.000,83.000,67.000,71.000,82.000,72.000,83.000,82.000,80.000,87.000,79.000,85.000,82.000,82.000,78.000,84.000,86.000,68.000,84.000,85.000,89.000,85.000,89.000,88.000,88.000,85.000,90.000,75.000,90.000,86.000,83.000,85.000,84.000,78.000,69.000,86.000,78.000,83.000,85.000,80.000,86.000,52.000,87.000,68.000,89.000,87.000,88.000,90.000,75.000,88.000,87.000,90.000,86.000,76.000,92.000,83.000,83.000,86.000,87.000,82.000,67.000,86.000,74.000,83.000,90.000,83.000,86.000,84.000,88.000,85.000,64.000,57.000,88.000,87.000,71.000,84.000,81.000,89.000,81.000,73.000,88.000,78.000,83.000,70.000,82.000,84.000,90.000,80.000,91.000,57.000};
		double[] b = new double[100];
		int i;
		Random r = new Random();
		for(i = 0; i < 100; ++i) {
			b[i] = r.nextGaussian();
		}
		//e.setInputArray(b);
		Engine e = new Engine();
		e.setInputArray(array);
		if(e.testNormalDistribution()) {
			System.out.println("成绩符合正态分布");
		} else {
			System.out.println("成绩不符合正态分布");
			if(e.getSkewness() > 0) {
				//偏度系数大于0
				System.out.println("成绩呈正偏态分布");
			} else {
				//偏度系数小于0
				System.out.println("成绩呈负偏态分布");
			}
			if(e.getKurtosis() > 0) {
				//峰度系数大于0
				System.out.println("成绩呈陡峭型分布");
			} else {
				//峰度系数小于0
				System.out.println("成绩呈平坡型分布");
			}
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException
	{
		test();
	}
}
class MyNumberAxis extends NumberAxis {
	private static final long serialVersionUID = -3091517955373751780L;
	int start, step;
	public MyNumberAxis(String title, int start, int step) {
		super(title);
		this.start = start;
		this.step = step;
	}
	@Override
	public List<NumberTick> refreshTicks(java.awt.Graphics2D g2,
            AxisState state,
            java.awt.geom.Rectangle2D dataArea,
            org.jfree.ui.RectangleEdge edge){
		List<NumberTick> list = new ArrayList<NumberTick>();
		int i;
		NumberTick nt = null;
		String label;
		for(i = start; i <= 100; i += step) {
			label = i == start ? "0" : Integer.toString(i);
			nt = new NumberTick(i, label, TextAnchor.TOP_CENTER, TextAnchor.CENTER, 0);
			list.add(nt);
		}
		if(i - 100 < step) {
			nt = new NumberTick(i, "100", TextAnchor.TOP_CENTER, TextAnchor.CENTER, 0);
			list.add(nt);
		}
		return list;
	}
}
