/*
  This code is part of FCPTools - an FCP-based client library for Freenet

  CopyLeft (c) 2001 by David McNab

  Developers:
  - David McNab <david@rebirthing.co.nz>
  - Jay Oliveri <ilnero@gmx.net>
  
  Currently maintained by Jay Oliveri <ilnero@gmx.net>
  
  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

#include "ezFCPlib.h"

#include <time.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <fcntl.h>

#include "ez_sys.h"

/* On success, return the number of bytes written */
int fcpWriteKey(hFCP *hfcp, char *buf, int len)
{
	int count;
	int bytes;
	int rc;

	/* While there's still data to write from caller.. */
	bytes = 0;
	while (len) {

		count = (len > 8192 ? 8192 : len);
		rc = _fcpWrite(hfcp->key->tmpblock->fd, buf, count);
		
		if (rc != count) {
			_fcpLog(FCP_LOG_DEBUG, "error during call to fcpWriteKey()");
			return -1;
		}

		/* Info was written.. update indexes */
		hfcp->key->size += rc;
		bytes += rc;

		len -= rc;
		buf += rc;
	}
	
	return bytes;
}


int fcpWriteMetadata(hFCP *hfcp, char *buf, int len)
{
	int count;
	int bytes;
	int rc;

	/* While there's still data to write from caller.. */
	bytes = 0;
	while (len) {

		count = (len > 8192 ? 8192 : len);
		rc = _fcpWrite(hfcp->key->metadata->tmpblock->fd, buf, count);
		
		if (rc != count) {
			_fcpLog(FCP_LOG_DEBUG, "error during call to fcpWriteMetadata()");
			return -1;
		}

		/* Info was written.. update indexes */
		hfcp->key->metadata->size += rc;
		bytes += rc;

		len -= rc;
		buf += rc;
	}
	
	return bytes;
}

