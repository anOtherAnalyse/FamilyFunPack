package family_fun_pack.utils;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.IllegalAccessException;
import java.lang.NoSuchFieldException;
import java.lang.reflect.Field;

@OnlyIn(Dist.CLIENT)
public interface ReflectUtils {

  public static <T> T getFieldValue(Object obj, String[] field_names) {
    Field field = null;

    int i = 0;
    while(field == null && i < field_names.length) {
      try {
        field = obj.getClass().getDeclaredField(field_names[i]);
      } catch(NoSuchFieldException e) {}
      i ++;
    }

    if(field == null) throw new RuntimeException(String.format("No field %s in %s", field_names[0], obj.getClass().toString()));

    field.setAccessible(true);

    T out = null;
    try {
      out = (T) field.get(obj);
    } catch(IllegalAccessException e) {
      throw new RuntimeException(String.format("Illegal access of field %s in %s", field_names[0], obj.getClass().toString()));
    }

    return out;
  }

}
