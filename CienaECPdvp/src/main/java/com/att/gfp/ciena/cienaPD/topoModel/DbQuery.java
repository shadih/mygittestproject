package com.att.gfp.ciena.cienaPD.topoModel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;

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
