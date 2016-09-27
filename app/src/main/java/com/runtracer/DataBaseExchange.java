package com.runtracer;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class DataBaseExchange implements Serializable, Cloneable {
	private static final long serialVersionUID = 100L;

	URL url;
	String command = "";
	String accountEmail = "";
	String full_name = "";
	String hash= "";

	JSONObject json_data_in = new JSONObject();
	JSONObject json_data_out = new JSONObject();

	public int error_no=0;
	public boolean pending= false;

	/**
	 * Creates and returns a copy of this {@code Object}. The default
	 * implementation returns a so-called "shallow" copy: It creates a new
	 * instance of the same class and then copies the field values (including
	 * object references) from this instance to the new instance. A "deep" copy,
	 * in contrast, would also recursively clone nested objects. A subclass that
	 * needs to implement this kind of cloning should call {@code super.clone()}
	 * to create the new instance and then create deep copies of the nested,
	 * mutable objects.
	 *
	 * @return a copy of this object.
	 * @throws CloneNotSupportedException if this object's class does not implement the {@code
	 *                                    Cloneable} interface.
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	protected String getHash() {
		String hash= null;
		try {
			hash = this.clone().toString();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		assert hash != null;
		this.hash= String.valueOf((hash.hashCode()));

		return (this.hash);
	}

	public static DataBaseExchange createDataBaseExchange() {
		return new DataBaseExchange();
	}

	public void clear() {
		try {
			url= new URL("https://www.runtrace.com");
			command = "empty";
			accountEmail = "empty";
			full_name = "empty";
			json_data_in = new JSONObject("{\"key\":\"data\"}");
			json_data_out= new JSONObject("{\"key\":\"data\"}");
			error_no=0;
			pending= false;
		} catch (JSONException | MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(this.url);
		out.writeObject(this.command);
		out.writeObject(this.accountEmail);
		out.writeObject(this.full_name);
		out.writeObject(this.hash);

		out.writeObject(this.json_data_in.toString());
		out.writeObject(this.json_data_out.toString());
		out.writeObject(this.error_no);
		out.writeObject(this.pending);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException, JSONException {
		this.url= (URL) in.readObject();
		this.command= (String) in.readObject();
		this.accountEmail= (String) in.readObject();
		this.full_name= (String) in.readObject();
		this.hash= (String) in.readObject();

		this.json_data_in= new JSONObject((String) in.readObject());
		this.json_data_out= new JSONObject((String) in.readObject());
		this.error_no= (int) in.readObject();
		this.pending= (boolean) in.readObject();
	}
}
