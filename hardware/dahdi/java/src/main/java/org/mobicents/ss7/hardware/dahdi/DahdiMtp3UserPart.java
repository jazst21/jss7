/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */ 
package org.mobicents.ss7.hardware.dahdi;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.mobicents.protocols.ss7.mtp.Mtp3UserPart;

/**
 * @author amit bhayani
 *
 */
public class DahdiMtp3UserPart implements Mtp3UserPart {

	/* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.mtp.Mtp3UserPart#read(java.nio.ByteBuffer)
	 */
	@Override
	public int read(ByteBuffer arg0) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.mtp.Mtp3UserPart#write(java.nio.ByteBuffer)
	 */
	@Override
	public int write(ByteBuffer arg0) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void execute() throws IOException {
		// TODO Auto-generated method stub
		
	}

}