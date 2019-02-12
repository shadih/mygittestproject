package com.att.gfp.ciena.cienaPD.topoModel;


import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class DbQuery
{
	private static GraphDatabaseService db;
	static
	{
        	db = new GraphDatabaseFactory().newEmbeddedDatabase("/Users/th555j/ws/hpdb/dbstore");
    	}
	
	public static GraphDatabaseService getGraphDB() throws Exception
	{
		return db;
    	}
	
}
