package core;

import picocli.CommandLine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "idf", mixinStandardHelpOptions = true,
        version = "0.0.1",
        description = "for now asdf")
public class CLI implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        BufferedImage img = ImageIO.read(path.toFile());
        //TODO: add path as cli option
        img = Exif.applyOrientation(img, path);   // fix orientation once
        img = Gray.toGray(img);                   // then proceed to hashing/resize
        System.out.println("test");
        return 0;
    }
}

//TODO: implement
