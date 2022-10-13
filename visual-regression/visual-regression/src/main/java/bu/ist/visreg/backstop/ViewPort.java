package bu.ist.visreg.backstop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ViewPort {
	String label;
	Integer width;
	Integer height;
	
	List<String> invalidMessages;
	
	public String getLabel() {
		return label;
	}
	public ViewPort setLabel(String label) {
		this.label = label;
		return this;
	}
	public Integer getWidth() {
		return width;
	}
	public ViewPort setWidth(Integer width) {
		this.width = width;
		return this;
	}
	public Integer getHeight() {
		return height;
	}
	public ViewPort setHeight(Integer height) {
		this.height = height;
		return this;
	}
	@JsonIgnore
	public boolean isValid() {
		if(invalidMessages == null) {
			getInvalidMessages();
			return isValid();
		}
		return invalidMessages.isEmpty();
	}
	@JsonIgnore
	public Collection<? extends String> getInvalidMessages() {
		if(invalidMessages != null) {
			return invalidMessages;
		}
		invalidMessages = new ArrayList<String>();
		String missing = "viewport missing parameter: ";
		if(label == null || label.isEmpty()) invalidMessages.add(missing + "label");
		if(width == null || width == 0) invalidMessages.add(missing + "width");
		if(height == null || height == 0) invalidMessages.add(missing + "height");
		return invalidMessages;
	}
	public static ViewPort getDefaultInstance() {
		return new ViewPort()
				.setLabel("laptop")
				.setWidth(1440)
				.setHeight(900);
	}
}