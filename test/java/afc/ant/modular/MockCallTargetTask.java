/* Copyright (c) 2013, Dźmitry Laŭčuk
   All rights reserved.

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are met: 

   1. Redistributions of source code must retain the above copyright notice, this
      list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
   ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
   ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
   (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
   ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */
package afc.ant.modular;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.taskdefs.CallTarget;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.PropertySet;

import junit.framework.Assert;

public class MockCallTargetTask extends CallTarget
{
    public Throwable exception;
    public boolean executed;
    
    public MockProject ownProject;
    
    public boolean inheritAll;
    public boolean inheritRefs;
    public String target;
    
    private final ArrayList<Property> params = new ArrayList<Property>();
    private final ArrayList<PropertySet> propertySets = new ArrayList<PropertySet>();
    
    public MockCallTargetTask(final Project project)
    {
        setProject(project);
        ownProject = new MockProject();
    }
    
    @Override
    public void execute()
    {
        Assert.assertFalse(executed);
        executed = true;
        
        // lightweight execute; filling own project properties with those overridden via params and property sets
        for (final Property param : params) {
            param.setProject(ownProject);
            param.execute();
        }
        
        final PropertyHelper helper = PropertyHelper.getPropertyHelper(ownProject);
        
        for (final PropertySet propSet : propertySets) {
            for (final Map.Entry<Object, Object> prop : propSet.getProperties().entrySet()) {
                helper.setProperty((String) prop.getKey(), prop.getValue(), false);
            }
        }
        
        if (inheritAll)
        {
            for (final Map.Entry prop : (Set<Map.Entry>) getProject().getProperties().entrySet()) {
                final String propertyKey = (String) prop.getKey();
                if (propertyKey.equals("basedir")) {
                    continue;
                }
                helper.setNewProperty("", (String) prop.getKey(), prop.getValue());
            }
        }
        
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        if (exception instanceof Error) {
            throw (Error) exception;
        }
    }
    
    @Override
    public void setTarget(final String target)
    {
        this.target = target;
        super.setTarget(target);
    }
    
    @Override
    public void setInheritAll(final boolean inheritAll)
    {
        this.inheritAll = inheritAll;
        super.setInheritAll(inheritAll);
    }
    
    @Override
    public void setInheritRefs(final boolean inheritRefs)
    {
        this.inheritRefs = inheritRefs;
        super.setInheritRefs(inheritRefs);
    }
    
    @Override
    public Property createParam()
    {
        final Property param = super.createParam();
        param.setProject(ownProject);
        params.add(param);
        return param;
    }
    
    @Override
    public void addPropertyset(final PropertySet propertySet)
    {
        propertySets.add(propertySet);
        super.addPropertyset(propertySet);
    }
}
