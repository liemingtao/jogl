/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package jogamp.opengl.util.glsl;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLException;

import jogamp.opengl.util.GLFixedArrayHandlerFlat;

import com.jogamp.opengl.util.GLArrayDataEditable;
import com.jogamp.opengl.util.GLArrayHandler;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * Interleaved GLSL arrays, i.e. where this buffer data 
 * represents many arrays. 
 */
public class GLSLArrayHandlerInterleaved implements GLArrayHandler {
  private GLArrayDataEditable ad;
  private ShaderState st;
  private List<GLArrayHandler> subArrays = new ArrayList<GLArrayHandler>();

  public GLSLArrayHandlerInterleaved(ShaderState st, GLArrayDataEditable ad) {
    this.st = st;
    this.ad = ad;
  }

  public final void addSubHandler(GLArrayHandler handler) {
      subArrays.add(handler);
  }

  private final void syncSubData(GL gl, boolean enable) {
      for(int i=0; i<subArrays.size(); i++) {
          subArrays.get(i).syncData(gl, enable);
      }      
  }
  
  public final void syncData(GL gl, boolean enable) {
    GL2ES2 glsl = gl.getGL2ES2();

    if(enable) {
        Buffer buffer = ad.getBuffer();

        /*
         * This would be the non optimized code path:
         * 
        if(ad.isVBO()) {
            glsl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
            if(!ad.isVBOWritten()) {
                if(null!=buffer) {
                    glsl.glBufferData(ad.getVBOTarget(), ad.getSizeInBytes(), buffer, ad.getVBOUsage());
                }
                ad.setVBOWritten(true);
            }
        }
        syncSubData(gl, true);
        */
        if(ad.isVBO()) {
            // bind and refresh the VBO / vertex-attr only if necessary
            if(!ad.isVBOWritten()) {
                glsl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
                if(null!=buffer) {
                    glsl.glBufferData(ad.getVBOTarget(), ad.getSizeInBytes(), buffer, ad.getVBOUsage());
                }
                ad.setVBOWritten(true);
                syncSubData(gl, true);
            } else if(st.getAttribLocation(glsl, ad) >= 0) {
                // didn't experience a performance hit on this query ..
                // (using ShaderState's location query above to validate the location)
                final int[] qi = new int[1];
                glsl.glGetVertexAttribiv(ad.getLocation(), GL2ES2.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING, qi, 0);
                if(ad.getVBOName() != qi[0]) {
                    glsl.glBindBuffer(ad.getVBOTarget(), ad.getVBOName());
                    syncSubData(gl, true);
                }
            }
        } else if(null!=buffer) {
            syncSubData(gl, true);
        }
    } else {
        syncSubData(gl, false);
        if(ad.isVBO()) {
            glsl.glBindBuffer(ad.getVBOTarget(), 0);
        }
    }      
  }
  
  public final void enableState(GL gl, boolean enable) {
    for(int i=0; i<subArrays.size(); i++) {
        subArrays.get(i).enableState(gl, enable);
    }      
  }

}
