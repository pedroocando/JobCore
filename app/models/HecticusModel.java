package models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.db.ebean.Model;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
@SuppressWarnings("serial")
public abstract class HecticusModel extends Model {
	/**
	 * Metodo para retornar la data como un json, es necesario para aquellos modelos que tienen un objeto interno que establece una relacion.
	 * ObjectNode no puede resolver los ciclos en las relaciones y si se intenta insertar un modelo en un ObjectNode explota porque se queda
	 * resolviendo la relacion hasta el infinito. La solucion sin cambiar la version de la libreria actual es usar este metodo
	 * 
	 * @return un objeto con todos los valores del modelo 
	 */
	public abstract ObjectNode toJson();
}
