package com.dynamo.cr.sceneed.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.vecmath.Matrix4d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Test;

import com.dynamo.cr.editor.core.IResourceType;
import com.dynamo.cr.editor.core.IResourceTypeRegistry;

public class NodeTest extends AbstractNodeTest {

    private DummyNode node;
    private DummyNodeLoader loader;

    @Override
    @Before
    public void setup() throws CoreException, IOException {
        super.setup();

        this.loader = new DummyNodeLoader();

        String extension = "dummy";

        // Mock as if dummy was a registered resource type
        IResourceType resourceType = mock(IResourceType.class);
        when(resourceType.getTemplateData()).thenReturn(new byte[0]);

        IResourceTypeRegistry registry = mock(IResourceTypeRegistry.class);
        when(registry.getResourceTypeFromExtension(extension)).thenReturn(resourceType);

        setResourceTypeRegistry(registry);

        this.node = registerAndLoadRoot(DummyNode.class, extension, this.loader);
    }

    @Test
    public void testSetProperty() throws ExecutionException {
        assertEquals(0, getNodeProperty(this.node, "dummyProperty"));
        setNodeProperty(this.node, "dummyProperty", 1);
        assertEquals(1, getNodeProperty(this.node, "dummyProperty"));
        undo();
        assertEquals(0, getNodeProperty(this.node, "dummyProperty"));
    }

    @Test
    public void testSetDynamicProperty() throws ExecutionException {
        assertEquals(0, getNodeProperty(this.node, DummyNode.DYNAMIC_PROPERTY));
        assertFalse(isNodePropertyOverridden(this.node, DummyNode.DYNAMIC_PROPERTY));
        setNodeProperty(this.node, DummyNode.DYNAMIC_PROPERTY, 1);
        assertEquals(1, getNodeProperty(this.node, DummyNode.DYNAMIC_PROPERTY));
        assertTrue(isNodePropertyOverridden(this.node, DummyNode.DYNAMIC_PROPERTY));
        undo();
        assertEquals(0, getNodeProperty(this.node, DummyNode.DYNAMIC_PROPERTY));
        assertFalse(isNodePropertyOverridden(this.node, DummyNode.DYNAMIC_PROPERTY));
    }

    @Test
    public void testBug1148() throws ExecutionException {
        Matrix4d transform = new Matrix4d();
        transform.setColumn(3, new Vector4d(10, 20, 30, 1));
        transform.set(new Quat4d(0, 1, 0, 0));
        node.setLocalTransform(transform);
        assertEquals(new Vector3d(0, 180, 0), node.getEuler());
    }

}
