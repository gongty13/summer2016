package org.apache.hadoop.BHW;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.hadoop.BHW.GIF.AnimatedGifEncoder;
import org.apache.hadoop.BHW.physics.ParticleSystem;

public class GIFGenerator {
	/**
	 * 把多张jpg图片合成一张  
	 * @param pic String[] 多个jpg文件名 包含路径  
	 * @param newPic String 生成的gif文件名 包含路径  
	 */  
	public  static void jpgToGif(String pic[], String newPic) {  
		try {  
			AnimatedGifEncoder e = new AnimatedGifEncoder();  //请见<a href="http://http://blog.csdn.net/ycb1689/article/details/8071733">本博客文章  </a>
			e.setRepeat(0);  
			e.start(newPic);  
			BufferedImage src[] = new BufferedImage[pic.length];  
			for (int i = 0; i < src.length; i++) {  
				e.setDelay((int)(1000*ParticleSystem.deltaT)); //设置播放的延迟时间  
				src[i] = ImageIO.read(new File(pic[i])); // 读入需要播放的jpg文件  
				e.addFrame(src[i]);  //添加到帧中  
			}  
			e.finish();  
		} catch (Exception e) {  
			System.out.println( "jpgToGif Failed:");  
			e.printStackTrace();  
		}  
	}  
	public static void run(float time, String gifPath){
		String[] pics= new String[(int)(time/ParticleSystem.deltaT)+1];
		for(int i=0;i<=time/ParticleSystem.deltaT;++i)
			pics[i]="./"+(float)(i*ParticleSystem.deltaT)+".JPEG";
		jpgToGif(pics, gifPath);
	}
}
