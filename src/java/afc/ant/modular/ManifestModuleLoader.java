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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Path.PathElement;

public class ManifestModuleLoader extends ProjectComponent implements ModuleLoader
{
    private static final Name ATTRIB_DEPENDENCIES = new Name("Depends");
    
    private static final Pattern listElementPattern = Pattern.compile("\\S+");
    
    private final ArrayList<ClasspathAttribute> classpathAttributes = new ArrayList<ClasspathAttribute>();
    
    public String normalisePath(final String path)
    {
        return ModuleUtil.normalisePath(path, getProject().getBaseDir());
    }
    
    public ModuleInfo loadModule(final String path) throws ModuleNotLoadedException
    {
        final String normalisedPath = normalisePath(path);
        final Attributes attributes = readManifestBuildSection(normalisedPath);
        // TODO Test that the normalised path is assigned to ModuleInfo instances.
        final ModuleInfo moduleInfo = new ModuleInfo(normalisedPath);
        
        addDependencies(attributes, moduleInfo);
        addClasspathAttributes(attributes, moduleInfo);
        // merging remaining attributes without modification
        for (final Map.Entry<Object, Object> entry : attributes.entrySet()) {
            final Name key = (Name) entry.getKey();
            moduleInfo.addAttribute(key.toString(), entry.getValue());
        }
        return moduleInfo;
    }
    
    private static void addDependencies(final Attributes attributes, final ModuleInfo moduleInfo)
            throws ModuleNotLoadedException
    {
        final String deps = (String) attributes.remove(ATTRIB_DEPENDENCIES);
        if (deps == null) {
            return;
        }
        final Matcher m = listElementPattern.matcher(deps);
        while (m.find()) {
            final String url = m.group();
            
            final String dependeeModulePath;
            try {
                dependeeModulePath = decodeUrl(url);
            }
            catch (RuntimeException ex) {
                throw new ModuleNotLoadedException(MessageFormat.format(
                    "Unable to load the module ''{1}''. This dependee module path is an invalid URL: ''{0}''.",
                        m.group(), moduleInfo.getPath()), ex);
            }
            
            moduleInfo.addDependency(dependeeModulePath);
        }
    }
    
    private void addClasspathAttributes(final Attributes attributes, final ModuleInfo moduleInfo)
            throws ModuleNotLoadedException
    {
        for (final ClasspathAttribute attrib : classpathAttributes) {
            final String attributeName = attrib.name;
            if (attributeName == null) {
                throw new BuildException("A 'classpathAttribute' element with undefined name is encountered.");
            }
            final String value = (String) attributes.remove(new Name(attributeName));
            if (value == null) {
                continue;
            }
            final Path classpath = new Path(getProject());
            final Matcher m = listElementPattern.matcher(value);
            while (m.find()) {
                final String url = m.group();
                
                final String classpathElement;
                try {
                    classpathElement = decodeUrl(url);
                }
                catch (RuntimeException ex) {
                    throw new ModuleNotLoadedException(MessageFormat.format(
                            "Unable to load the module ''{2}''. The classpath attribute ''{1}'' " +
                            "contains an invalid URL element: ''{0}''.",
                            m.group(), attributeName, moduleInfo.getPath()), ex);
                }
                
                final PathElement element = classpath.createPathElement();
                element.setPath(moduleInfo.getPath() + classpathElement);
            }
            moduleInfo.addAttribute(attributeName, classpath);
        }
    }
    
    private Attributes readManifestBuildSection(final String path) throws ModuleNotLoadedException
    {
        final File moduleDir = new File(getProject().getBaseDir(), path);
        if (!moduleDir.exists()) {
            throw new ModuleNotLoadedException(MessageFormat.format(
                    "The module ''{0}'' (''{1}'') does not exist.", path, moduleDir.getAbsolutePath()));
        }
        if (!moduleDir.isDirectory()) {
            throw new ModuleNotLoadedException(MessageFormat.format(
                    "The module path ''{0}'' (''{1}'') is not a directory.", path, moduleDir.getAbsolutePath()));
        }
        
        final File manifestFile = new File(moduleDir, "META-INF/MANIFEST.MF");
        if (!manifestFile.exists()) {
            throw new ModuleNotLoadedException(MessageFormat.format(
                    "The module ''{0}'' does not have the manifest (''{1}'').",
                    path, manifestFile.getAbsolutePath()));
        }
        if (!manifestFile.isFile()) {
            throw new ModuleNotLoadedException(MessageFormat.format(
                    "The module ''{0}'' has the manifest that is not a file (''{1}'').",
                    path, manifestFile.getAbsolutePath()));
        }
        
        try {
            final FileInputStream in = new FileInputStream(manifestFile);
            try {
                final Manifest manifest = new Manifest(in);
                final String buildSectionName = "Build";
                final Attributes buildAttributes = manifest.getAttributes(buildSectionName);
                if (buildAttributes == null) {
                    throw new ModuleNotLoadedException(MessageFormat.format(
                            "The module ''{0}'' does not have the ''{2}'' section in its manifest (''{1}'').",
                            path, manifestFile.getAbsolutePath(), buildSectionName));
                }
                return buildAttributes;
            }
            finally {
                in.close();
            }
        }
        catch (IOException ex) {
            throw new ModuleNotLoadedException(MessageFormat.format(
                    "An I/O error is encountered while loading the manifest of the module ''{0}'' (''{1}'').",
                    path, manifestFile.getAbsolutePath()), ex);
        }
    }
    
    public static class ClasspathAttribute
    {
        private String name;
        
        public void setName(final String name)
        {
            this.name = name;
        }
    }
    
    public ClasspathAttribute createClasspathAttribute()
    {
        final ClasspathAttribute val = new ClasspathAttribute();
        classpathAttributes.add(val);
        return val;
    }
    
    // RuntimeException indicates that an error is encountered while decoding this URL.
    private static String decodeUrl(final String encodedUrl) throws ModuleNotLoadedException, RuntimeException
    {
        try {
            return URLDecoder.decode(encodedUrl, "utf-8");
        }
        catch (UnsupportedEncodingException ex) {
            throw new ModuleNotLoadedException("The encoding 'UTF-8' is not supported by this JVM.", ex);
        }
    }
}
