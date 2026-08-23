//Copyright (c) 2003, David Brackeen
//All rights reserved.
//
//Redistribution and use in source and binary forms, with or without modification, are permitted
//provided that the following conditions are met:
//Redistributions of source code must retain the above copyright notice, this list of conditions
//and the following disclaimer.
//Redistributions in binary form must reproduce the above copyright notice, this list of
//conditions and the following disclaimer in the documentation and/or other materials provided
//with the distribution.
//Neither the name of David Brackeen nor the names of its contributors may be used to endorse or
//promote products derived from this software without specific prior written permission.
//
//THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
//IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
//AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
//CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
//SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED ANDON ANY
//THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
//OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
//POSSIBILITY OF SUCH DAMAGE.

package org.unicog.numberrace.sound;

/**
	The Sound class is a container for sound samples. The sound
	samples are format-agnostic and are stored as a byte array.
*/
public class Sound {

    private byte[] samples;
	private String key;

    /**
       Create a new Sound object with the specified byte array.
       The array is not copied.
    */
    public Sound(String key, byte[] samples) {
	this.samples = samples;
	this.key = key;
    }

    /**
    	Returns this Sound's objects samples as a byte array.
    */
    public byte[] getSamples() {
        return samples;
    }


    public String getKey() {
	return key;
    }

}
