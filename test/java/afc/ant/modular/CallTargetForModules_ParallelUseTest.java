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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;

import afc.ant.modular.CallTargetForModules.ModuleElement;
import afc.ant.modular.CallTargetForModules.ParamElement;

import junit.framework.TestCase;

// TODO add high load tests
public class CallTargetForModules_ParallelUseTest extends TestCase
{
    private CallTargetForModules task;
    private MockProject project;
    private MockModuleLoader moduleLoader;
    
    @Override
    protected void setUp()
    {
        project = new MockProject();
        task = new CallTargetForModules();
        task.setProject(project);
        moduleLoader = new MockModuleLoader();
    }
    
    @Override
    protected void tearDown()
    {
        moduleLoader = null;
        task = null;
        project = null;
    }
    
    public void testParallelRun_TwoThreads_MultipleModulesWithDeps_RelatedHierarchies_ModulePropertyDefined()
    {
        // Unambiguous order of module processing is selected for the sake of simplicity.
        final ModuleInfo moduleInfo = new ModuleInfo("foo/");
        moduleInfo.addAttribute("1", "2");
        moduleInfo.addDependency("bar/");
        moduleInfo.addDependency("baz/");
        final ModuleInfo dep1 = new ModuleInfo("bar/");
        dep1.addDependency("baz/");
        final ModuleInfo moduleInfo2 = new ModuleInfo("baz/");
        moduleInfo2.addAttribute("qq", "ww");
        moduleInfo2.addAttribute("aa", "ss");
        moduleInfo2.addDependency("quux/");
        final ModuleInfo dep2 = new ModuleInfo("quux/");
        dep2.addAttribute("z", "x");
        
        moduleLoader.modules.put("foo/", moduleInfo);
        moduleLoader.modules.put("bar/", dep1);
        moduleLoader.modules.put("baz/", moduleInfo2);
        moduleLoader.modules.put("quux/", dep2);
        
        final MockCallTargetTask task1 = new MockCallTargetTask(project);
        project.tasks.add(task1);
        final MockCallTargetTask task2 = new MockCallTargetTask(project);
        project.tasks.add(task2);
        final MockCallTargetTask task3 = new MockCallTargetTask(project);
        project.tasks.add(task3);
        final MockCallTargetTask task4 = new MockCallTargetTask(project);
        project.tasks.add(task4);
        
        task.init();
        task.setTarget("someTarget");
        task.setModuleProperty("mProp");
        task.createModule().setPath("foo");
        task.createModule().setPath("baz");
        task.addConfigured(moduleLoader);
        task.setThreadCount(2);
        
        final ParamElement param = task.createParam();
        param.setName("p");
        param.setValue("o");
        
        project.setProperty("qwerty", "board");
        
        task.perform();
        
        TestUtil.assertCallTargetState(task1, true, "someTarget", true, false, "mProp", dep2,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task2, true, "someTarget", true, false, "mProp", moduleInfo2,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task3, true, "someTarget", true, false, "mProp", dep1,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task4, true, "someTarget", true, false, "mProp", moduleInfo,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
    }
    
    public void testParallelRun_TwoThreads_MultipleModulesWithDeps_RelatedHierarchies_ModulePropertyUndefined()
    {
        // Unambiguous order of module processing is selected for the sake of simplicity.
        final ModuleInfo moduleInfo = new ModuleInfo("foo/");
        moduleInfo.addAttribute("1", "2");
        moduleInfo.addDependency("bar/");
        moduleInfo.addDependency("baz/");
        final ModuleInfo dep1 = new ModuleInfo("bar/");
        dep1.addDependency("baz/");
        final ModuleInfo moduleInfo2 = new ModuleInfo("baz/");
        moduleInfo2.addAttribute("qq", "ww");
        moduleInfo2.addAttribute("aa", "ss");
        moduleInfo2.addDependency("quux/");
        final ModuleInfo dep2 = new ModuleInfo("quux/");
        dep2.addAttribute("z", "x");
        
        moduleLoader.modules.put("foo/", moduleInfo);
        moduleLoader.modules.put("bar/", dep1);
        moduleLoader.modules.put("baz/", moduleInfo2);
        moduleLoader.modules.put("quux/", dep2);
        
        final MockCallTargetTask task1 = new MockCallTargetTask(project);
        project.tasks.add(task1);
        final MockCallTargetTask task2 = new MockCallTargetTask(project);
        project.tasks.add(task2);
        final MockCallTargetTask task3 = new MockCallTargetTask(project);
        project.tasks.add(task3);
        final MockCallTargetTask task4 = new MockCallTargetTask(project);
        project.tasks.add(task4);
        
        task.init();
        task.setTarget("someTarget");
        task.createModule().setPath("foo");
        task.createModule().setPath("baz");
        task.addConfigured(moduleLoader);
        task.setThreadCount(2);
        
        final ParamElement param = task.createParam();
        param.setName("p");
        param.setValue("o");
        
        project.setProperty("qwerty", "board");
        
        task.perform();
        
        TestUtil.assertCallTargetState(task1, true, "someTarget", true, false,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task2, true, "someTarget", true, false,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task3, true, "someTarget", true, false,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task4, true, "someTarget", true, false,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
    }
    
    public void testParallelRun_TwoThreads_MultipleModulesWithDeps_UnrelatedHierarchies_ModulePropertyDefined()
    {
        final ModuleInfo moduleInfo = new ModuleInfo("foo/");
        moduleInfo.addAttribute("1", "2");
        moduleInfo.addDependency("bar/");
        moduleInfo.addDependency("baz/");
        final ModuleInfo dep1 = new ModuleInfo("bar/");
        
        final ModuleInfo moduleInfo2 = new ModuleInfo("baz/");
        moduleInfo2.addAttribute("qq", "ww");
        moduleInfo2.addAttribute("aa", "ss");
        moduleInfo2.addDependency("quux/");
        final ModuleInfo dep2 = new ModuleInfo("quux/");
        dep2.addAttribute("z", "x");
        
        moduleLoader.modules.put("foo/", moduleInfo);
        moduleLoader.modules.put("bar/", dep1);
        moduleLoader.modules.put("baz/", moduleInfo2);
        moduleLoader.modules.put("quux/", dep2);
        
        final MockCallTargetTask task1 = new MockCallTargetTask(project);
        project.tasks.add(task1);
        final MockCallTargetTask task2 = new MockCallTargetTask(project);
        project.tasks.add(task2);
        final MockCallTargetTask task3 = new MockCallTargetTask(project);
        project.tasks.add(task3);
        final MockCallTargetTask task4 = new MockCallTargetTask(project);
        project.tasks.add(task4);
        
        /* Verifying that module-specific targets to not see the changes of each other.
           Support of MockCallTargetTask is needed for correct emulation. */
        task1.propertiesToSet = Collections.<String, Object>singletonMap("task1", "prop1");
        task2.propertiesToSet = Collections.<String, Object>singletonMap("task2", "prop2");
        task3.propertiesToSet = Collections.<String, Object>singletonMap("task3", "prop3");
        task4.propertiesToSet = Collections.<String, Object>singletonMap("task4", "prop4");
        
        task.init();
        task.setTarget("someTarget");
        task.setModuleProperty("mProp");
        task.createModule().setPath("foo");
        task.createModule().setPath("baz");
        task.addConfigured(moduleLoader);
        task.setThreadCount(2);
        
        final ParamElement param = task.createParam();
        param.setName("p");
        param.setValue("o");
        
        project.setProperty("qwerty", "board");
        
        task.perform();
        
        final Map<String, ModuleInfo> moduleInfos = TestUtil.<String, ModuleInfo>map(
                moduleInfo.getPath(), moduleInfo,
                moduleInfo2.getPath(), moduleInfo2,
                dep1.getPath(), dep1,
                dep2.getPath(), dep2);
        final ArrayList<String> modulePaths = new ArrayList<String>();
        modulePaths.add(TestUtil.getModulePath(task1.ownProject, "mProp"));
        modulePaths.add(TestUtil.getModulePath(task2.ownProject, "mProp"));
        modulePaths.add(TestUtil.getModulePath(task3.ownProject, "mProp"));
        modulePaths.add(TestUtil.getModulePath(task4.ownProject, "mProp"));
        
        assertTrue(modulePaths.contains("foo/"));
        assertTrue(modulePaths.contains("bar/"));
        assertTrue(modulePaths.contains("baz/"));
        assertTrue(modulePaths.contains("quux/"));
        assertTrue(modulePaths.indexOf("foo/") > modulePaths.indexOf("bar/"));
        assertTrue(modulePaths.indexOf("baz/") > modulePaths.indexOf("quux/"));
        
        TestUtil.assertCallTargetState(task1, true, "someTarget", true, false, "mProp",
                moduleInfos.get(modulePaths.get(0)),
                TestUtil.<String, Object>map("qwerty", "board", "p", "o", "task1", "prop1"));
        TestUtil.assertCallTargetState(task2, true, "someTarget", true, false, "mProp",
                moduleInfos.get(modulePaths.get(1)),
                TestUtil.<String, Object>map("qwerty", "board", "p", "o", "task2", "prop2"));
        TestUtil.assertCallTargetState(task3, true, "someTarget", true, false, "mProp",
                moduleInfos.get(modulePaths.get(2)),
                TestUtil.<String, Object>map("qwerty", "board", "p", "o", "task3", "prop3"));
        TestUtil.assertCallTargetState(task4, true, "someTarget", true, false, "mProp",
                moduleInfos.get(modulePaths.get(3)),
                TestUtil.<String, Object>map("qwerty", "board", "p", "o", "task4", "prop4"));
    }
    
    public void testParallelRun_TwoThreads_MultipleModulesWithDeps_UnrelatedHierarchies_ModulePropertyUndefined()
    {
        // Unambiguous order of module processing is selected for the sake of simplicity.
        final ModuleInfo moduleInfo = new ModuleInfo("foo/");
        moduleInfo.addAttribute("1", "2");
        moduleInfo.addDependency("bar/");
        moduleInfo.addDependency("baz/");
        final ModuleInfo dep1 = new ModuleInfo("bar/");
        
        final ModuleInfo moduleInfo2 = new ModuleInfo("baz/");
        moduleInfo2.addAttribute("qq", "ww");
        moduleInfo2.addAttribute("aa", "ss");
        moduleInfo2.addDependency("quux/");
        final ModuleInfo dep2 = new ModuleInfo("quux/");
        dep2.addAttribute("z", "x");
        
        moduleLoader.modules.put("foo/", moduleInfo);
        moduleLoader.modules.put("bar/", dep1);
        moduleLoader.modules.put("baz/", moduleInfo2);
        moduleLoader.modules.put("quux/", dep2);
        
        final MockCallTargetTask task1 = new MockCallTargetTask(project);
        project.tasks.add(task1);
        final MockCallTargetTask task2 = new MockCallTargetTask(project);
        project.tasks.add(task2);
        final MockCallTargetTask task3 = new MockCallTargetTask(project);
        project.tasks.add(task3);
        final MockCallTargetTask task4 = new MockCallTargetTask(project);
        project.tasks.add(task4);
        
        /* Verifying that module-specific targets to not see the changes of each other.
           Support of MockCallTargetTask is needed for correct emulation. */
        task1.propertiesToSet = Collections.<String, Object>singletonMap("task1", "prop1");
        task2.propertiesToSet = Collections.<String, Object>singletonMap("task2", "prop2");
        task3.propertiesToSet = Collections.<String, Object>singletonMap("task3", "prop3");
        task4.propertiesToSet = Collections.<String, Object>singletonMap("task4", "prop4");
        
        task.init();
        task.setTarget("someTarget");
        task.createModule().setPath("foo");
        task.createModule().setPath("baz");
        task.addConfigured(moduleLoader);
        task.setThreadCount(2);
        
        final ParamElement param = task.createParam();
        param.setName("p");
        param.setValue("o");
        
        project.setProperty("qwerty", "board");
        
        task.perform();
        
        /* Since no module is passed to the targets there is not way to determine
           what targets were called for what modules. */
        TestUtil.assertCallTargetState(task1, true, "someTarget", true, false,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o", "task1", "prop1"));
        TestUtil.assertCallTargetState(task2, true, "someTarget", true, false,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o", "task2", "prop2"));
        TestUtil.assertCallTargetState(task3, true, "someTarget", true, false,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o", "task3", "prop3"));
        TestUtil.assertCallTargetState(task4, true, "someTarget", true, false,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o", "task4", "prop4"));
    }
    
    public void testParallelRun_ModuleWithDeps_BuildFailure()
    {
        // Unambiguous order of module processing is selected for the sake of simplicity.
        final ModuleInfo moduleInfo = new ModuleInfo("foo/");
        moduleInfo.addAttribute("1", "2");
        moduleInfo.addDependency("bar/");
        moduleInfo.addDependency("baz/");
        final ModuleInfo dep1 = new ModuleInfo("bar/");
        dep1.addDependency("baz/");
        final ModuleInfo dep2 = new ModuleInfo("baz/");
        dep2.addAttribute("qq", "ww");
        dep2.addAttribute("aa", "ss");
        
        moduleLoader.modules.put("foo/", moduleInfo);
        moduleLoader.modules.put("bar/", dep1);
        moduleLoader.modules.put("baz/", dep2);
        
        final MockCallTargetTask task1 = new MockCallTargetTask(project);
        project.tasks.add(task1);
        final MockCallTargetTask task2 = new MockCallTargetTask(project);
        project.tasks.add(task2);
        final MockCallTargetTask task3 = new MockCallTargetTask(project);
        project.tasks.add(task3);
        
        final Location location = new Location("some_file", 10, 20);
        final BuildException exception = new BuildException("test_failure_msg", location);
        task2.exception = exception;
        
        task.init();
        task.setTarget("someTarget");
        task.setModuleProperty("moduleProp");
        task.createModule().setPath("foo");
        task.addConfigured(moduleLoader);
        task.setThreadCount(2);
        
        final ParamElement param = task.createParam();
        param.setName("p");
        param.setValue("o");
        
        project.setProperty("qwerty", "board");
        
        try {
            task.perform();
            fail();
        }
        catch (BuildException ex) {
            assertEquals("Module 'bar/': test_failure_msg", ex.getMessage());
            assertSame(exception, ex.getCause());
            assertSame(location, ex.getLocation());
            assertTrue(Arrays.equals(exception.getStackTrace(), ex.getStackTrace()));
        }
        
        TestUtil.assertCallTargetState(task1, true, "someTarget", true, false, "moduleProp", dep2,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task2, true, "someTarget", true, false, "moduleProp", dep1,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        assertFalse(task3.executed);
    }
    
    public void testParallelRun_ModuleWithDeps_RuntimeExceptionInATarget()
    {
        // Unambiguous order of module processing is selected for the sake of simplicity.
        final ModuleInfo moduleInfo = new ModuleInfo("foo/");
        moduleInfo.addAttribute("1", "2");
        moduleInfo.addDependency("bar/");
        moduleInfo.addDependency("baz/");
        final ModuleInfo dep1 = new ModuleInfo("bar/");
        dep1.addDependency("baz/");
        final ModuleInfo dep2 = new ModuleInfo("baz/");
        dep2.addAttribute("qq", "ww");
        dep2.addAttribute("aa", "ss");
        
        moduleLoader.modules.put("foo/", moduleInfo);
        moduleLoader.modules.put("bar/", dep1);
        moduleLoader.modules.put("baz/", dep2);
        
        final MockCallTargetTask task1 = new MockCallTargetTask(project);
        project.tasks.add(task1);
        final MockCallTargetTask task2 = new MockCallTargetTask(project);
        project.tasks.add(task2);
        final MockCallTargetTask task3 = new MockCallTargetTask(project);
        project.tasks.add(task3);
        
        final RuntimeException exception = new RuntimeException("test_failure_msg");
        task2.exception = exception;
        
        task.init();
        task.setTarget("someTarget");
        task.setModuleProperty("moduleProp");
        task.createModule().setPath("foo");
        task.addConfigured(moduleLoader);
        task.setThreadCount(2);
        
        final ParamElement param = task.createParam();
        param.setName("p");
        param.setValue("o");
        
        project.setProperty("qwerty", "board");
        
        try {
            task.perform();
            fail();
        }
        catch (BuildException ex) {
            assertEquals("Module 'bar/': " + ex.getCause().getMessage(), ex.getMessage());
            assertNotNull(ex.getLocation());
        }
        
        TestUtil.assertCallTargetState(task1, true, "someTarget", true, false, "moduleProp", dep2,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task2, true, "someTarget", true, false, "moduleProp", dep1,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        assertFalse(task3.executed);
    }
    
    public void testParallelRun_MultipleModulesWithDeps_CustomTarget_ModulePropertyDefined()
    {
        // Unambiguous order of module processing is selected for the sake of simplicity.
        final ModuleInfo moduleInfo = new ModuleInfo("foo/");
        moduleInfo.addAttribute("1", "2");
        moduleInfo.addDependency("bar/");
        moduleInfo.addDependency("baz/");
        final ModuleInfo dep1 = new ModuleInfo("bar/");
        dep1.addDependency("baz/");
        final ModuleInfo moduleInfo2 = new ModuleInfo("baz/");
        moduleInfo2.addAttribute("qq", "ww");
        moduleInfo2.addAttribute("aa", "ss");
        moduleInfo2.addDependency("quux/");
        final ModuleInfo dep2 = new ModuleInfo("quux/");
        dep2.addAttribute("z", "x");
        
        moduleLoader.modules.put("foo/", moduleInfo);
        moduleLoader.modules.put("bar/", dep1);
        moduleLoader.modules.put("baz/", moduleInfo2);
        moduleLoader.modules.put("quux/", dep2);
        
        final MockCallTargetTask task1 = new MockCallTargetTask(project);
        project.tasks.add(task1);
        final MockCallTargetTask task2 = new MockCallTargetTask(project);
        project.tasks.add(task2);
        final MockCallTargetTask task3 = new MockCallTargetTask(project);
        project.tasks.add(task3);
        final MockCallTargetTask task4 = new MockCallTargetTask(project);
        project.tasks.add(task4);
        
        task.init();
        task.setTarget("someTarget");
        task.setModuleProperty("mProp");
        task.createModule().setPath("foo");
        final ModuleElement moduleElem = task.createModule();
        moduleElem.setPath("baz");
        moduleElem.setTarget("customTarget");
        final ModuleElement moduleElem2 = task.createModule();
        moduleElem2.setPath("bar");
        moduleElem2.setTarget("customTarget2");
        task.addConfigured(moduleLoader);
        task.setThreadCount(2);
        
        final ParamElement param = task.createParam();
        param.setName("p");
        param.setValue("o");
        
        project.setProperty("qwerty", "board");
        
        task.perform();
        
        TestUtil.assertCallTargetState(task1, true, "someTarget", true, false, "mProp", dep2,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task2, true, "customTarget", true, false, "mProp", moduleInfo2,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task3, true, "customTarget2", true, false, "mProp", dep1,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task4, true, "someTarget", true, false, "mProp", moduleInfo,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
    }
    
    public void testParallelRun_MultipleModulesWithDeps_CustomTarget_ModulePropertyUndefined()
    {
        // Unambiguous order of module processing is selected for the sake of simplicity.
        final ModuleInfo moduleInfo = new ModuleInfo("foo/");
        moduleInfo.addAttribute("1", "2");
        moduleInfo.addDependency("bar/");
        moduleInfo.addDependency("baz/");
        final ModuleInfo dep1 = new ModuleInfo("bar/");
        dep1.addDependency("baz/");
        final ModuleInfo moduleInfo2 = new ModuleInfo("baz/");
        moduleInfo2.addAttribute("qq", "ww");
        moduleInfo2.addAttribute("aa", "ss");
        moduleInfo2.addDependency("quux/");
        final ModuleInfo dep2 = new ModuleInfo("quux/");
        dep2.addAttribute("z", "x");
        
        moduleLoader.modules.put("foo/", moduleInfo);
        moduleLoader.modules.put("bar/", dep1);
        moduleLoader.modules.put("baz/", moduleInfo2);
        moduleLoader.modules.put("quux/", dep2);
        
        final MockCallTargetTask task1 = new MockCallTargetTask(project);
        project.tasks.add(task1);
        final MockCallTargetTask task2 = new MockCallTargetTask(project);
        project.tasks.add(task2);
        final MockCallTargetTask task3 = new MockCallTargetTask(project);
        project.tasks.add(task3);
        final MockCallTargetTask task4 = new MockCallTargetTask(project);
        project.tasks.add(task4);
        
        task.init();
        task.setTarget("someTarget");
        task.createModule().setPath("foo");
        final ModuleElement moduleElem = task.createModule();
        moduleElem.setPath("baz");
        moduleElem.setTarget("customTarget");
        task.addConfigured(moduleLoader);
        task.setThreadCount(2);
        
        final ParamElement param = task.createParam();
        param.setName("p");
        param.setValue("o");
        
        project.setProperty("qwerty", "board");
        
        task.perform();
        
        TestUtil.assertCallTargetState(task1, true, "someTarget", true, false,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task2, true, "customTarget", true, false,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task3, true, "someTarget", true, false,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
        TestUtil.assertCallTargetState(task4, true, "someTarget", true, false,
                TestUtil.<String, Object>map("qwerty", "board", "p", "o"));
    }
}
