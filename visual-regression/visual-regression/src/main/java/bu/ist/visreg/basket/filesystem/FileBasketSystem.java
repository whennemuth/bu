package bu.ist.visreg.basket.filesystem;

import java.io.File;

import bu.ist.visreg.backstop.BasicBackstopSplitter;
import bu.ist.visreg.basket.Basket;
import bu.ist.visreg.basket.Basket.BasketEnum;
import bu.ist.visreg.basket.BasketItemSplitter;
import bu.ist.visreg.basket.BasketSystem;
import bu.ist.visreg.util.ArgumentParser;

public class FileBasketSystem extends BasketSystem {

	private File rootDirectory;
	
	public FileBasketSystem(String rootLocation) {
		super.rootLocation = rootLocation;
		this.rootDirectory = new File(rootLocation);
	}

	@Override
	public void load(BasketItemSplitter splitter) throws Exception {
		
		getRootDirectory();
		
		for(Basket.BasketEnum be : BasketEnum.values()) {
			
			FileBasket basket = new FileBasket(be, this);
			
			basket.createIfNotExists();
			
			basket.load(splitter);
			
			addBasket(basket);
		}
	}

	public File getRootDirectory() {
		return rootDirectory;
	}
	
	public static void main(String[] args) throws Exception {
		ArgumentParser parser = new ArgumentParser(args);
		if(parser.has("r|root")) {
			FileBasketSystem fbs = new FileBasketSystem(parser.getString("r|root"));
			fbs.load(new BasicBackstopSplitter());
			System.out.println(fbs.toString());
		}
		else {
			System.err.println("Missing parameter: \"-r|--root\"");
		}
	}
}
