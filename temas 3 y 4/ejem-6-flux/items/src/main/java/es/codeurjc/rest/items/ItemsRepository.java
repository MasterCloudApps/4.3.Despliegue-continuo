package es.codeurjc.rest.items;

import java.time.LocalTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class ItemsRepository {

    private Map<Long, Item> items = new ConcurrentHashMap<>();
	private AtomicLong lastId = new AtomicLong();

	public ItemsRepository(){
        // Create some items

		Item item1 = new Item();
		item1.setDescription("Leche");
		item1.setChecked(true);
        this.postItem(item1);
        
        Item item2= new Item();
		item2.setDescription("Huevos");
		item2.setChecked(false);
		this.postItem(item2);

        Item item3= new Item();
		item3.setDescription("Time : "+LocalTime.now());
		item3.setChecked(false);
		this.postItem(item3);
    }
    
    public Collection<Item> getAllItems(){
        return items.values();
    }

    public Item getItem(Long id){
        return items.get(id);
    }

    public Item postItem(Item item){
        long id = lastId.incrementAndGet();
		item.setId(id);
		items.put(id, item);
		return item;
    }

    public Item putItem(Long id, Item itemActualizado){
        Item item = items.get(id);

		if (item != null) {
			itemActualizado.setId(id);
			items.put(id, itemActualizado);
			return itemActualizado;
		} else {
			return null;
		}
    }

    public Item removeItem(Long id){
        Item item = items.remove(id);
        return item;
    }

}