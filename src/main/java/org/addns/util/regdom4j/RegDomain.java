/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (C) 2012 Open Source Solution Technology Corporation
 * Copyright (C) 2012 HAMANO Tsukasa <hamano@osstech.co.jp>
 */

package org.addns.util.regdom4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * RegDomain
 * @author YahocenMiniPC
 */
public class RegDomain {

    public String getRegisteredDomain(String fqdn) {
        List<String> list = Arrays.asList(fqdn.split("\\."));
        LinkedList<String> parts = new LinkedList<>(list);
        return findRegisteredDomain(parts, EffectiveTldTree.tree);
    }

    private String findRegisteredDomain(LinkedList<String> parts,
                                        HashMap<String, HashMap> node){
        if(parts.isEmpty()){
            return null;
        }
        String sub = parts.removeLast();
        String result = null;
        if(node.get("!") != null){
            return "";
        }else if(node.get(sub) != null){
            result = findRegisteredDomain(parts, node.get(sub));
        }else if(node.get("*") != null){
            result = findRegisteredDomain(parts, node.get("*"));
        }else{
            return sub;
        }
        if(result == null){
            return null;
        }else if (result.equals("")) {
            return sub;
        }else{
            return result + "." + sub;
        }
    }

    public static void main(String[] args) {
        RegDomain regdom = new RegDomain();
        System.out.println(regdom.getRegisteredDomain("xxxx.sh.gov.cn"));
    }

}
