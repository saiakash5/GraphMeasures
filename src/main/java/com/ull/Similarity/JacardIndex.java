/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ull.Similarity;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.streaming.SimpleEdgeStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.types.NullValue;
import org.apache.flink.util.Collector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

/**
 * Single-pass, insertion-only exact Triangle Local and Global Count algorithm.
 * <p>
 * Based on http://www.kdd.org/kdd2016/papers/files/rfp0465-de-stefaniA.pdf.
 */

public class JacardIndex {
        
	public static void main(String[] args) throws Exception {

                System.setOut(new PrintStream("/Users/Akash/Desktop/output.log"));
		if (!parseParameters(args)) {
			return;
		}
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		SimpleEdgeStream<Integer, NullValue> edges = getGraphStream(env);
                DataStream<Edge<Integer, NullValue>> timed = edges.getEdges().rebalance()
                        .assignTimestampsAndWatermarks(new AscendingTimestampExtractor<Edge<Integer, NullValue>>() {

                                @Override
                                public long extractAscendingTimestamp(Edge element) {
                                    return System.currentTimeMillis();
                                }
                        });
             
             AllWindowedStream<Edge<Integer, NullValue>, TimeWindow> result2 =timed.keyBy(0).windowAll(TumblingEventTimeWindows.of(Time.hours(2)));
             result2.apply(new Jacard()).writeAsText("/Users/Akash/Desktop/text.txt",FileSystem.WriteMode.OVERWRITE);
             env.execute("Jacard Index");
	}
        
        private static class Jacard implements AllWindowFunction<Edge<Integer, NullValue>, String, TimeWindow> 
        {
            int k=2;
            TreeMap<Integer,TreeMap<Integer,Double>> Hash_values = new TreeMap<>();
            TreeMap<Integer,TreeMap<Integer,Integer>> Hash_position = new TreeMap<>();
            @Override
            public void apply(TimeWindow w, Iterable<Edge<Integer, NullValue>> input, Collector<String> out) throws Exception 
            {
                
                System.out.println("MYTEST::"+w.toString()+" --- "+w.maxTimestamp()+" ---- "+input.toString());
                
                int source = 11216;
                int destination = 1219;
                TreeMap<Integer,Integer> source_tm = new TreeMap<>();
                TreeMap<Integer,Integer> destination_tm = new TreeMap<>();
                ArrayList<Integer> l1 = new ArrayList<>();
                ArrayList<Integer> l2 = new ArrayList<>();
                for(Edge<Integer,NullValue> i : input)
                {
                    int source_u = i.f0;
                    int target_v = i.f1;

                    
                    for(int j = 1;j<=k;j++)
                    {
                        double hash_source_u = HashFunction1(source_u,j);
                        double hash_target_v = HashFunction1(target_v,j);
                        
                        if(Hash_values.containsKey(source_u))
                        {
                            TreeMap<Integer,Double> t_hash_values = Hash_values.get(source_u);
                            TreeMap<Integer,Integer> t_hash_position = Hash_position.get(source_u);
                            Double value = t_hash_values.get(j); 
                            if(hash_target_v< value)
                            {
                                t_hash_values.replace(j, hash_target_v);
                                Hash_values.replace(j, t_hash_values);
                                t_hash_position.replace(j, target_v);
                                Hash_position.replace(source_u, t_hash_position);
                            }
                            
                        }
                        else
                        {
                            TreeMap<Integer,Double> temp_hash = new TreeMap<>();
                            TreeMap<Integer,Integer> temp_position = new TreeMap<>();
                            for(int p=1;p<=k;p++)
                            {
                                temp_hash.put(p, Double.POSITIVE_INFINITY);
                            }
                            temp_hash.replace(j, hash_target_v);
                            for(int p=1;p<=k;p++)
                            {
                                temp_position.put(p, -1);
                            }
                            temp_position.replace(j, target_v);
                            Hash_values.put(source_u, temp_hash);
                            Hash_position.put(source_u, temp_position);
                            
                        }
                        
                        
                        
                        
                        if(Hash_values.containsKey(target_v))
                        {
                            TreeMap<Integer,Double> t_hash_values = Hash_values.get(target_v);
                            TreeMap<Integer,Integer> t_hash_position = Hash_position.get(target_v);
                            Double value = t_hash_values.get(j); 
                            if(hash_source_u< value)
                            {
                                t_hash_values.replace(j, value, hash_source_u);
                                Hash_values.replace(j, t_hash_values);
                                t_hash_position.replace(j, source_u);
                                Hash_position.replace(target_v, t_hash_position);
                            }
                        }
                        else
                        {
                                TreeMap<Integer,Double> temp_hash = new TreeMap<>();
                                TreeMap<Integer,Integer> temp_position = new TreeMap<>();
                                for(int p=1;p<=k;p++)
                                {
                                    temp_hash.put(p, Double.POSITIVE_INFINITY);
                                }
                                temp_hash.replace(j, hash_source_u);
                                for(int p=1;p<=k;p++)
                                {
                                    temp_position.put(p, -1);
                                }
                                temp_position.replace(j, source_u);
                                Hash_values.put(target_v, temp_hash);
                                Hash_position.put(target_v, temp_position);
                        }
                        
                 
                    }
                }
                
                for(int i : Hash_position.keySet())
                {
                    if(source == i)
                    {
                        out.collect(w.toString()+"from source match");
                        source_tm = Hash_position.get(i);
                        for(int j:source_tm.keySet())
                        {
                            
                            l1.add(source_tm.get(j));
                        }
                        
                    }
                    if(destination==i)
                    {
                        out.collect(w.toString()+"from dest block");
                        destination_tm = Hash_position.get(i);
                        for(int j:destination_tm.keySet())
                        {
                            l2.add(destination_tm.get(j));
                        }
                    }
                }
                int length = 0;
                ArrayList<Integer> l3 = new ArrayList<>();
                try{
                if(l1.size()>0 && l2.size()>0)
                {
                    out.collect(w.toString()+"from try block");
                    for(int i=1 ;i<=l2.size();i++)
                    {
                        if(l2.contains(l1.get(i)))
                        {
                            if(!l3.contains(i))
                                l3.add(i);
                            
                        }
                    }
                    
                }
                }
                catch(Exception e)
                {
                    out.collect("Edge does not exist until now");
                }
                if(l3.size()>0)
                {
                    for(int i=1;i<=l3.size();i++)
                    {
                        int x = l3.get(i);
                        int min = Math.min(Collections.frequency(l1,x),Collections.frequency(l2,x));
                        if(min>length)
                        {
                            length=min;
                        }
                    }
                }
                out.collect("similarity-->"+Integer.toString(length));
            }
        }
        
        public static double HashFunction1(int y,int k)
        {
            Random r1 = new Random();
            r1.setSeed((long) (+1*Math.pow(k,2+k)*1000));
            double hash_z = (double)1-(r1.nextDouble()/y);
            
            return hash_z;
        }
        
       
	// *** Transformation Methods *** //

	/**
	 * Receives 2 tuples from the same edge (src + target) and intersects the attached neighborhoods.
	 * For each common neighbor, increase local and global counters.
	 */
	public static final class IntersectNeighborhoods implements
			FlatMapFunction<Tuple3<Integer, Integer, TreeSet<Integer>>, Tuple2<Integer, Integer>> {

		Map<Tuple2<Integer, Integer>, TreeSet<Integer>> neighborhoods = new HashMap<>();

		public void flatMap(Tuple3<Integer, Integer, TreeSet<Integer>> t, Collector<Tuple2<Integer, Integer>> out) {
			//intersect neighborhoods and emit local and global counters
			Tuple2<Integer, Integer> key = new Tuple2<>(t.f0, t.f1);
			if (neighborhoods.containsKey(key)) {
				// this is the 2nd neighborhood => intersect
				TreeSet<Integer> t1 = neighborhoods.remove(key);
				TreeSet<Integer> t2 = t.f2;
				int counter = 0;
				if (t1.size() < t2.size()) {
					// iterate t1 and search t2
					for (int i : t1) {
						if (t2.contains(i)) {
							counter++;
							out.collect(new Tuple2<>(i, 1));
						}
					}
				} else {
					// iterate t2 and search t1
					for (int i : t2) {
						if (t1.contains(i)) {
							counter++;
							out.collect(new Tuple2<>(i, 1));
						}
					}
				}
				if (counter > 0) {
					//emit counter for srcID, trgID, and total
					out.collect(new Tuple2<>(t.f0, counter));
					out.collect(new Tuple2<>(t.f1, counter));
					// -1 signals the total counter
					out.collect(new Tuple2<>(-1, counter));
				}
			} else {
				// first neighborhood for this edge: store and wait for next
				neighborhoods.put(key, t.f2);
			}
		}
	}
        
       private static class ExactNeighborhood implements 
               FlatMapFunction<Tuple2<Integer,TreeSet<Integer>>,TreeMap<Integer,ArrayList<String>>>
       {
           TreeMap<Integer,TreeSet<String>> Sketch = new TreeMap<>(); 
           int x1 = 1;
           int x2 = 5;
           int k = 20;
           TreeMap<Integer,ArrayList<Double>> hm1 = new TreeMap<>();
           TreeMap<Integer,ArrayList<String>> hm2 = new TreeMap<>();
           public void flatMap(Tuple2<Integer, TreeSet<Integer>> t, Collector<TreeMap<Integer, ArrayList<String>>> clctr) throws Exception 
           {
                //TreeSet<Double> 
                
                int source = t.f0;
                TreeSet<Integer> dests = t.f1;
                TreeSet<String> dests1 = new TreeSet<>();
                Iterator it1 = dests.iterator();
                TreeSet<Double> hash_values = new TreeSet<>();
                if(!hm1.containsKey(source))
                {
                    ArrayList<Double> ts1 = new ArrayList<>();
                    for(int i=0;i<k;i++)
                    {
                        ts1.add(i,Double.POSITIVE_INFINITY);
                    }
                    hm1.put(source,ts1 );
                }
                if(!hm2.containsKey(source))
                {
                    ArrayList<String> ts2 = new ArrayList<>();
                    for(int i=0;i<k;i++)
                    {
                        ts2.add(i,null);
                    }
                    hm2.put(source, ts2);
                }                   
                for(int dest : dests)
                {
                    for(int i=0;i<k;i++)
                    {
                        Random r1 = new Random();
                        r1.setSeed((long) (+k*Math.pow((i+1),2)*1000));
                        double h = r1.nextDouble();
                        double hv1 = (double)1-(h/dest);
                        if(hm1.containsKey(source))
                        {
                            ArrayList<Double> ts1 = hm1.get(source);
                            ArrayList<String> ts2 = hm2.get(source);
                            if(hv1<ts1.get(i))
                            {
                                ts1.remove(i);
                                ts1.add(i, hv1);
                                ts2.remove(i);
                                ts2.add(i, Integer.toString(dest));
                            }
                        }
                        dests1.add(Integer.toString(dest)+" - "+Double.toString(hv1));
                    }               
                    Sketch.put(source, dests1);
                    clctr.collect(hm2);
                }
           }
       }
       
       
       
	// *************************************************************************
	//     UTIL METHODS
	// *************************************************************************

	private static boolean fileOutput = false;
	private static String edgeInputPath = null;
	private static String resultPath = null;

	private static boolean parseParameters(String[] args) {

		if (args.length > 0) {
			if (args.length != 2) {
				System.err.println("Usage: ExactTriangleCount <input edges path> <result path>");
				return false;
			}

			fileOutput = true;
			edgeInputPath = args[0];
			resultPath = args[1];
		} else {
			System.out.println("Executing ExactTriangleCount example with default parameters and built-in default data.");
			System.out.println("  Provide parameters to read input data from files.");
			System.out.println("  See the documentation for the correct format of input files.");
			System.out.println("  Usage: ExactTriangleCount <input edges path> <result path>");
		}
		return true;
	}


	@SuppressWarnings("serial")
	private static SimpleEdgeStream<Integer, NullValue> getGraphStream(StreamExecutionEnvironment env) 
        {
            return new SimpleEdgeStream<>(env.readTextFile("/Users/Akash/Documents/main_drive/ull_notes/csce649/dataset/set1.txt")
		.flatMap(new FlatMapFunction<String, Edge<Integer, NullValue>>() 
                {
                    @Override
                    public void flatMap(String s, Collector<Edge<Integer, NullValue>> out) 
                    {
                        String[] fields = s.split("\\s");
			if (!fields[0].equals("%")) 
                        {
                            int src = Integer.parseInt(fields[0]);
                            int trg = Integer.parseInt(fields[1]);
                            out.collect(new Edge<>(src, trg, NullValue.getInstance()));
			}
                    }
		}), env);

	}
}