package bu.ist.visreg.basket.filesystem;

import java.io.File;

import bu.ist.visreg.basket.Basket;
import bu.ist.visreg.basket.BasketItem;
import bu.ist.visreg.util.FileUtils;

public class FileBasketItem extends BasketItem {
	
	private FileUtils file;
	
	public FileBasketItem(Basket basket, String pathname, String content) {
		super(basket, pathname, content);
		file = new FileUtils(pathname);
	}

	@Override
	public void commitBasketMove(Basket nextBasket) throws Exception {
		
		if(file.isFile()) {
			File targetFolder = ((FileBasket) nextBasket).getFolder();
			File targetFile = new File(targetFolder.getAbsolutePath() + "\\" + file.getName());
			boolean commited = file.renameTo(targetFile);
			if(!commited) {
				throw new RuntimeException(String.format(
						"ERROR! Failed to move %s to %s",
						file.getAbsolutePath(),
						targetFolder));
			}
			else {
				this.pathname = targetFile.getAbsolutePath();
				System.out.println(String.format(
						"Moved %s to %s",
						file.getAbsolutePath(),
						targetFolder));
			}
		}
	}

	@Override
	public BasketItem getSplitItem(String json, String id) {
		if(id.equals(pathname)) {
			// The id IS the existing pathname. So, just change the content of this basket item to json and return it.
			return new FileBasketItem(basket, pathname, json);
		}
		else {
			// The id designates a portion of a new pathname to be based on the existing one.
			// Build the new pathname and return a corresponding basket item with json as the content.
			String newPathName = getExtendedPathname(id);
			return new FileBasketItem(basket, newPathName, json);
		}		
	}

	@Override
	public boolean persist() throws Exception {		
		return new FileUtils(new File(pathname)).writeFile(getContent());
	}

	@Override
	public boolean delete() {
		return new FileUtils(new File(pathname)).delete();
	}

	public void setFile(FileUtils file) {
		this.file = file;
	}
	
}
