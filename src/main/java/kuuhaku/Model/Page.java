package kuuhaku.Model;

import com.sun.istack.internal.NotNull;
import kuuhaku.Enum.PageType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class Page {
	private final PageType type;
	private final Object content;

	public Page(@NotNull PageType type, @NotNull Object content) {
		this.type = type;
		this.content = content;
	}

	public PageType getType() {
		return type;
	}

	public Object getContent() {
		return content;
	}

	@Override
	public String toString() {
		if (type == PageType.TEXT) {
			return ((Message) content).getContentRaw();
		} else {
			return ((MessageEmbed) content).getDescription();
		}
	}
}
