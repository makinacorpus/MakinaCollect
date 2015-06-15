package com.makina.collect.model;

@Deprecated
public final class Form {
    
    private int id;
    private String form_id;
    private String name;
    private String description;
    private String file_path;
    private String directory_path;
    
    public Form(int id, String form_id, String name, String description, String file_path, String directory_path)
    {
    	this.id=id;
    	this.form_id=form_id;
    	this.name=name;
    	this.description=description;
    	this.file_path=file_path;
    	this.directory_path=directory_path;
    }

	public int getId() {
		return id;
	}

	public String getForm_id() {
		return form_id;
	}
	
	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getFile_path() {
		return file_path;
	}

	public String getDirectory_path() {
		return directory_path;
	}
	
	
}
   
