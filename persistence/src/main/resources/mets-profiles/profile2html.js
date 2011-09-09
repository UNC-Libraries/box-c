				function ToggleChildren(n)
					{
					var cs = n.parentNode.childNodes;
					for (var i=0;i<cs.length;i++)
						{
						var c=cs.item(i);
						if (c.nodeType==1)
							{
							if (c.className=='xmlverb-element-container' || c.className=='xmlverb-text' || c.nodeName=='BR')
								{
								if (c.style.display=='none')
									{
									n.firstChild.nodeValue=String.fromCharCode(8211);
									c.style.display='inline';
									}
								else
									{
									n.firstChild.nodeValue='+';
									c.style.display='none';
									}
								}
							}
						}
					}

				function ToggleUpId(id)
					{
					var n;
					n = document.getElementById(id);
					if (n == null)
						{
						var c = document.getElementsByName(id);
						for (n in c)
							{
							ToggleUp(n);
							}
						}
					else
						{
						ToggleUp(n);
						}
					}
					
				function ToggleUp(n)
					{
					if (n.parentNode!=null)
						{
  					var cs = n.parentNode.childNodes;
  					for (var i=0;i<cs.length;i++)
  						{
  						var c=cs.item(i);
  						if (c.nodeType==1)
  							{
  							if (c.className=='xmlverb-element-container' || c.className=='xmlverb-text' || c.nodeName=='BR')
  								{
  								n.firstChild.nodeValue=String.fromCharCode(8211);
  								c.style.display='inline';
  								}
  							}
  						}
  					var p = n.parentNode;
  					if (p!=null) {ToggleUp(p);}
  					}
					}
